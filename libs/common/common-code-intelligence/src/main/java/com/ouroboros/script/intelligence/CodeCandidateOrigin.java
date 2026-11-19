package com.ouroboros.script.intelligence;

/** Evidence source, independent of whether the requested application's environment is available. */
public enum CodeCandidateOrigin {
  APPLICATION_SNAPSHOT, DRAFT_INFERENCE, PLATFORM_REFERENCE
}
