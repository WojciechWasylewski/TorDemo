package org.example.tordemo.exitnode.api;

import org.example.tordemo.exitnode.api.response.TorExitNodeResponse;
import org.example.tordemo.exitnode.service.TorExitNodeService;
import org.example.tordemo.exitnode.validation.IPv4;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TorNodeCheckerController {

    private final TorExitNodeService torExitNodeService;

    public TorNodeCheckerController(TorExitNodeService torExitNodeService) {
        this.torExitNodeService = torExitNodeService;
    }

    @GetMapping(path = "/{ip}")
    public ResponseEntity<TorExitNodeResponse> checkTorExitNodeByIp(@PathVariable @IPv4 String ip) {

        if (!torExitNodeService.isExitNode(ip)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(new TorExitNodeResponse(ip, true));
    }
}
