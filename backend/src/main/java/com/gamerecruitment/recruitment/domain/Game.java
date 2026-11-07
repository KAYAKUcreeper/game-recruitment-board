package com.gamerecruitment.recruitment.domain;

public enum Game {
  VALORANT("VALORANT"),
  APEX_LEGENDS("Apex Legends"),
  OVERWATCH("Overwatch");

  private final String displayName;

  Game(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return displayName;
  }
}
