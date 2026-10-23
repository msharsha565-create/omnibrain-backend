package com.omnibrain;

/**
 * PLACEHOLDER policy check: rejects any input containing the substring "pii". This is a keyword
 * filter, not PII detection; a real implementation would use pattern or model-based classifiers.
 */
public class GovernanceEngine {
  public static boolean allow(String input) {
    return !input.toLowerCase().contains("pii");
  }
}
