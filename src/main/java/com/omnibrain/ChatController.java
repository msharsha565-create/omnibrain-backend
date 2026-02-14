
package com.omnibrain;

import org.springframework.web.bind.annotation.*;

@RestController
public class ChatController {

  @PostMapping("/chat")
  public String chat(@RequestBody String msg) {
    if(!GovernanceEngine.allow(msg)) {
      return "REFUSED BY POLICY";
    }
    return "AI Response to: " + msg;
  }
}
