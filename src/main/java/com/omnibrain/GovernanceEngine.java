
package com.omnibrain;

import java.nio.file.*;

public class GovernanceEngine {
  public static boolean allow(String input) {
    return !input.toLowerCase().contains("pii");
  }
}
