package org.example.tordemo.exitnode.api;

import org.example.tordemo.exitnode.api.response.TorInfoResponse;
import org.example.tordemo.exitnode.service.TorExitNodeService;
import org.example.tordemo.exitnode.validation.IPv4;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TorNodeCheckerController {

    private final TorExitNodeService torExitNodeService;

    public TorNodeCheckerController(TorExitNodeService torExitNodeService) {
        this.torExitNodeService = torExitNodeService;
    }

    @RequestMapping(path = "/{ip}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkTorExitNode(@PathVariable @IPv4 String ip) {
        return torExitNodeService.isExitNode(ip) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping(path = "/{ip}")
    public ResponseEntity<TorInfoResponse> checkTorExitNodeByIp(@PathVariable @IPv4 String ip) {

        if (!torExitNodeService.isExitNode(ip)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(new TorInfoResponse(ip, true));
    }
}
