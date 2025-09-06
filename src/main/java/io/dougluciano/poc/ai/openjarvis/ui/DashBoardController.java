package io.dougluciano.poc.ai.openjarvis.ui;

import io.dougluciano.poc.ai.openjarvis.core.robot.RobotOrchestratorService;
import io.dougluciano.poc.ai.openjarvis.ui.sse.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequiredArgsConstructor
public class DashBoardController {

    // injeta a classe do robo via construtor
    private final RobotOrchestratorService robotOrchestratorService;
    //injeta o serviço de coleta de logs para exibição na textarea
    private final SseService sseService;

    /**
     * Mapeia a requisição para a raiz da aplicação (ex: http://localhost:8080/).
     * @return O nome do arquivo HTML ("dashboard") que o Thymeleaf deve buscar
     * na pasta 'src/main/resources/templates/'.
     */
    @GetMapping("/")
    public String showDashBoard(){
        return "dashboard";
    }

    @PostMapping("/start-robot")
    public String startRobotProcess(){
        robotOrchestratorService.startRobot();

        // Após iniciar o processo, redireciona o usuário para a página inicial.
        return "redirect:/";
    }

    @GetMapping("/log-stream")
    public SseEmitter logStream(){
        // definindo um timeoutgrande para o stream
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseService.addEmmitter(emitter);
        return emitter;
    }
}
