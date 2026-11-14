package com.dafatfish.afkmover;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public class AfkMoverClient implements ClientModInitializer {
    public static final String MOD_ID = "afkmover";
    
    // 移动状态枚举
    private enum MoveState {
        IDLE,
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        COMPLETE
    }
    
    private static MoveState currentState = MoveState.IDLE;
    private static int stateTimer = 0;
    private static int idleTicks = 0;
    private static BlockPos lastPos = null;
    private static final int AFK_THRESHOLD = 20 * 60; // 1分钟 = 20 ticks/秒 * 60秒 = 1200 ticks
    private static final int MOVE_DURATION = 10; // 每个方向移动的tick数（约0.5秒）
    
    @Override
    public void onInitializeClient() {
        System.out.println("[AFK Mover Fabric 1.21.1] 模组加载成功！1分钟不动自动移动～嘿嘿～");
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.player == null || client.level == null) return;
            
            LocalPlayer player = client.player;
            BlockPos currentPos = player.blockPosition();
            
            // 检测玩家是否移动
            if (lastPos != null && !lastPos.equals(currentPos)) {
                // 玩家移动了，重置计时器和状态
                idleTicks = 0;
                currentState = MoveState.IDLE;
                stateTimer = 0;
            } else {
                // 玩家没动
                idleTicks++;
            }
            lastPos = currentPos;
            
            // 如果玩家AFK超过阈值，开始自动移动
            if (idleTicks >= AFK_THRESHOLD && currentState == MoveState.IDLE) {
                startAutoMove();
            }
            
            // 执行自动移动状态机
            if (currentState != MoveState.IDLE) {
                executeAutoMove(player);
            }
        });
    }
    
    private void startAutoMove() {
        currentState = MoveState.FORWARD;
        stateTimer = 0;
        System.out.println("[AFK Mover Fabric 1.21.1] 检测到AFK，开始自动移动序列：前→后→左→右");
    }
    
    private void executeAutoMove(LocalPlayer player) {
        stateTimer++;
        
        switch (currentState) {
            case FORWARD -> {
                if (stateTimer <= MOVE_DURATION) {
                    player.setForwardInput(true);
                } else {
                    player.setForwardInput(false);
                    nextState(MoveState.BACKWARD);
                }
            }
            case BACKWARD -> {
                if (stateTimer <= MOVE_DURATION) {
                    player.setBackInput(true);
                } else {
                    player.setBackInput(false);
                    nextState(MoveState.LEFT);
                }
            }
            case LEFT -> {
                if (stateTimer <= MOVE_DURATION) {
                    player.setLeftInput(true);
                } else {
                    player.setLeftInput(false);
                    nextState(MoveState.RIGHT);
                }
            }
            case RIGHT -> {
                if (stateTimer <= MOVE_DURATION) {
                    player.setRightInput(true);
                } else {
                    player.setRightInput(false);
                    nextState(MoveState.COMPLETE);
                }
            }
            case COMPLETE -> {
                // 完成一轮，重置到IDLE，如果还在AFK会再次触发
                currentState = MoveState.IDLE;
                stateTimer = 0;
                idleTicks = 0; // 重置idle计时，让玩家有1分钟缓冲
                System.out.println("[AFK Mover Fabric 1.21.1] 自动移动序列完成，重新计时～");
            }
            default -> {}
        }
    }
    
    private void nextState(MoveState next) {
        currentState = next;
        stateTimer = 0;
    }
}