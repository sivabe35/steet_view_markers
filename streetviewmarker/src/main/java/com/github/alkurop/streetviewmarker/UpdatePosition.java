package com.github.alkurop.streetviewmarker;

public class UpdatePosition {
  public Location center;
  public long radius;

  public UpdatePosition(Location center, long radius) {
    this.center = center;
    this.radius = radius;
  }

}