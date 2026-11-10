package org.example.tordemo.exitnode.client;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TorExitNodeParser {

    public Set<String> parse(String torResponse) {
        return torResponse.lines()
                .filter(line -> line.startsWith("ExitAddress "))
                .map(line -> line.split("\\s+")[1])
                .limit(20)
                .collect(Collectors.toUnmodifiableSet());
    }
}
