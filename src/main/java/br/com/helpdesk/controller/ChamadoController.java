package br.com.helpdesk.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import br.com.helpdesk.model.Chamado;
import br.com.helpdesk.model.Usuario;
import br.com.helpdesk.repository.ChamadoRepository;
import br.com.helpdesk.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChamadoController {

    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;

    public ChamadoController(
            ChamadoRepository chamadoRepository,
            UsuarioRepository usuarioRepository) {

        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/")
public String inicio() {
    return "redirect:/chamados";
}

    @GetMapping("/chamados")
public String listar(Model model) {
    model.addAttribute(
            "chamados",
            chamadoRepository.listarComSolicitante());

    model.addAttribute(
            "resumoStatus",
            chamadoRepository.contarPorStatus());

    return "chamados/lista";
}

    @GetMapping("/chamados/novo")
    public String abrirFormulario(Model model) {
        model.addAttribute("cadastro", new CadastroChamado("", "", null));
        model.addAttribute("usuarios", usuarioRepository.findAll());

        return "chamados/novo";
    }

    @PostMapping("/chamados/novo")
    public String cadastrar(
            @Valid @ModelAttribute("cadastro") CadastroChamado cadastro,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirectAttributes) {

        model.addAttribute("usuarios", usuarioRepository.findAll());

        if (resultado.hasErrors()) {
            return "chamados/novo";
        }

        Usuario solicitante = usuarioRepository
                .findById(cadastro.usuarioId())
                .orElse(null);

        if (solicitante == null) {
            resultado.rejectValue(
                    "usuarioId",
                    "cadastro.usuarioId",
                    "Usuário não encontrado. Selecione um usuário cadastrado.");

            return "chamados/novo";
        }

        Chamado chamado = new Chamado();
        chamado.setTitulo(cadastro.titulo().strip());
        chamado.setDescricao(cadastro.descricao().strip());
        chamado.setSolicitante(solicitante);

        try {
            chamadoRepository.save(chamado);
        } catch (DataIntegrityViolationException ex) {
            resultado.reject(
                    "cadastro.erro",
                    "Não foi possível salvar o chamado. Verifique os dados e o solicitante.");

            return "chamados/novo";
        }

        redirectAttributes.addFlashAttribute(
                "mensagem",
                "Chamado cadastrado com sucesso.");

        return "redirect:/chamados";
    }

    @GetMapping("/chamados/{id}/editar")
public String abrirEdicao(@PathVariable("id") Long id, Model model) {
    Chamado chamado = buscarChamado(id);

    model.addAttribute("chamadoId", id);
    model.addAttribute("statusDisponiveis", Chamado.Status.values());
    model.addAttribute("edicao", new EdicaoChamado(
            chamado.getTitulo(),
            chamado.getDescricao(),
            chamado.getStatus()));

    return "chamados/editar";
}

@PostMapping("/chamados/{id}/editar")
public String editar(
        @PathVariable("id") Long id,
        @Valid @ModelAttribute("edicao") EdicaoChamado edicao,
        BindingResult resultado,
        Model model,
        RedirectAttributes redirectAttributes) {

    Chamado chamado = buscarChamado(id);

    model.addAttribute("chamadoId", id);
    model.addAttribute("statusDisponiveis", Chamado.Status.values());

    if (resultado.hasErrors()) {
        return "chamados/editar";
    }

    chamado.setTitulo(edicao.titulo().strip());
    chamado.setDescricao(edicao.descricao().strip());
    chamado.setStatus(edicao.status());

    try {
        chamadoRepository.save(chamado);
    } catch (DataIntegrityViolationException ex) {
        resultado.reject(
                "edicao.erro",
                "Não foi possível atualizar o chamado. Confira os dados.");

        return "chamados/editar";
    }

    redirectAttributes.addFlashAttribute(
            "mensagem",
            "Chamado atualizado com sucesso.");

    return "redirect:/chamados";
}

@PostMapping("/chamados/{id}/excluir")
public String excluir(
        @PathVariable("id") Long id,
        RedirectAttributes redirectAttributes) {

    Chamado chamado = buscarChamado(id);

    try {
        chamadoRepository.delete(chamado);
    } catch (DataIntegrityViolationException ex) {
        redirectAttributes.addFlashAttribute(
                "mensagem",
                "Não foi possível excluir o chamado porque ele está vinculado a outro registro.");

        return "redirect:/chamados";
    }

    redirectAttributes.addFlashAttribute(
            "mensagem",
            "Chamado excluído com sucesso.");

    return "redirect:/chamados";
}

private Chamado buscarChamado(Long id) {
    return chamadoRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Chamado não encontrado."));
}

public record EdicaoChamado(
        @NotBlank(message = "Informe o título.")
        @Size(max = 150, message = "O título deve ter até 150 caracteres.")
        String titulo,

        @NotBlank(message = "Informe a descrição.")
        @Size(max = 2000, message = "A descrição deve ter até 2000 caracteres.")
        String descricao,

        @NotNull(message = "Selecione o status.")
        Chamado.Status status) {
}

    public record CadastroChamado(
            @NotBlank(message = "Informe o título.")
            @Size(max = 150, message = "O título deve ter até 150 caracteres.")
            String titulo,

            @NotBlank(message = "Informe a descrição.")
            @Size(max = 2000, message = "A descrição deve ter até 2000 caracteres.")
            String descricao,

            @NotNull(message = "Selecione o usuário solicitante.")
            Long usuarioId) {
    }
}