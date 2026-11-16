package br.com.helpdesk.controller;

import br.com.helpdesk.model.Usuario;
import br.com.helpdesk.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller

public class UsuarioController {
    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/usuarios")
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        return "usuarios/lista";
    }

    @GetMapping("/usuarios/novo")
    public String abrirFormulario(Model model) {
        model.addAttribute("cadastro", new CadastroUsuario("", ""));
        return "usuarios/novo";
    }

    @PostMapping("/usuarios/novo")
    public String cadastrar(
            @Valid @ModelAttribute("cadastro") CadastroUsuario cadastro,
            BindingResult resultado,
            RedirectAttributes redirectAttributes) {

        if (resultado.hasErrors()) {
            return "usuarios/novo";
        }

        Usuario usuario = new Usuario();
        usuario.setNome(cadastro.nome().strip());
        usuario.setEmail(cadastro.email().strip());

        try {
            usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException ex) {
            resultado.rejectValue("email", "cadastro.email",
                    "Não foi possível salvar. Confira se esse e-mail já está cadastrado.");
            return "usuarios/novo";
        }

        redirectAttributes.addFlashAttribute("mensagem", "Usuário cadastrado com sucesso.");
        return "redirect:/usuarios/novo";
    }
    
    @GetMapping("/usuarios/{id}/editar")
    public String abrirEdicao(@PathVariable("id") Long id, Model model) {
        Usuario usuario = buscarUsuario(id);
        model.addAttribute("usuarioId", id);
        model.addAttribute("cadastro",
                new CadastroUsuario(usuario.getNome(), usuario.getEmail()));
        return "usuarios/novo";
    }

    @PostMapping("/usuarios/{id}/editar")
    public String editar(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("cadastro") CadastroUsuario cadastro,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = buscarUsuario(id);
        model.addAttribute("usuarioId", id);

        if (resultado.hasErrors()) {
            return "usuarios/novo";
        }

        usuario.setNome(cadastro.nome().strip());
        usuario.setEmail(cadastro.email().strip());

        try {
            usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException ex) {
            resultado.rejectValue("email", "cadastro.email",
                    "Não foi possível salvar. Confira se esse e-mail já está em uso.");
            return "usuarios/novo";
        }

        redirectAttributes.addFlashAttribute("mensagem", "Usuário atualizado com sucesso.");
        return "redirect:/usuarios";
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    public record CadastroUsuario(
            @NotBlank(message = "Informe o nome.")
            @Size(max = 120, message = "O nome deve ter até 120 caracteres.")
            String nome,

            @NotBlank(message = "Informe o e-mail.")
            @Email(message = "Informe um e-mail válido.")
            @Size(max = 254, message = "O e-mail deve ter até 254 caracteres.")
            String email) {
    }
}