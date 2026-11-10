package org.example.tordemo.exitnode.api;

import org.example.tordemo.exitnode.service.TorExitNodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TorNodeCheckerController {

    TorExitNodeService torExitNodeService;

    public TorNodeCheckerController(TorExitNodeService torExitNodeService) {
        this.torExitNodeService = torExitNodeService;
    }

    @RequestMapping(path = "/{ip}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkTorExitNode(@PathVariable String ip) {
        return torExitNodeService.isExitNode(ip) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
