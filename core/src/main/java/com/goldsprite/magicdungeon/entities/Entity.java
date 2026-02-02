package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.magicdungeon.utils.Constants;

public class Entity {
    public int x;
    public int y;
    public Color color;

    // Visual Position (Pixels)
    public float visualX;
    public float visualY;
    public float visualSpeed;

    // Animation Offsets
    public float bumpX;
    public float bumpY;

    public Entity(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;

        this.visualX = x * Constants.TILE_SIZE;
        this.visualY = y * Constants.TILE_SIZE;

        // Default visual speed
        this.visualSpeed = Constants.TILE_SIZE * 10;

        this.bumpX = 0;
        this.bumpY = 0;
    }

    public void updateVisuals(float dt) {
        // Smooth movement interpolation (Linear)
        float targetX = this.x * Constants.TILE_SIZE;
        float targetY = this.y * Constants.TILE_SIZE;

        float moveSpeed = this.visualSpeed;

        // X Axis
        if (this.visualX != targetX) {
            float dirX = Math.signum(targetX - this.visualX);
            float dist = Math.abs(targetX - this.visualX);
            float move = moveSpeed * dt;

            if (move >= dist) {
                this.visualX = targetX;
            } else {
                this.visualX += dirX * move;
            }
        }

        // Y Axis
        if (this.visualY != targetY) {
            float dirY = Math.signum(targetY - this.visualY);
            float dist = Math.abs(targetY - this.visualY);
            float move = moveSpeed * dt;

            if (move >= dist) {
                this.visualY = targetY;
            } else {
                this.visualY += dirY * move;
            }
        }

        // Bump animation decay
        float bumpDecay = 10;
        this.bumpX += (0 - this.bumpX) * bumpDecay * dt;
        this.bumpY += (0 - this.bumpY) * bumpDecay * dt;

        if (Math.abs(this.bumpX) < 0.1f) this.bumpX = 0;
        if (Math.abs(this.bumpY) < 0.1f) this.bumpY = 0;
    }

    public void triggerBump(int dirX, int dirY) {
        float force = Constants.TILE_SIZE * 0.5f;
        this.bumpX = dirX * force;
        this.bumpY = dirY * force;
    }
}
