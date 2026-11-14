package com.dafatfish.afkmover;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("afkmover")
public class AfkMoverMod {
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
    private static final int PAUSE_DURATION = 5; // 方向间暂停的tick数
    
    public AfkMoverMod() {
        // 只在客户端注册事件
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        System.out.println("[AFK Mover] 模组加载成功！1分钟不动自动移动～嘿嘿～");
    }
    
    @OnlyIn(Dist.CLIENT)
    private static class ClientEvents {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.level == null) return;
            
            LocalPlayer player = mc.player;
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
        }
        
        private void startAutoMove() {
            currentState = MoveState.FORWARD;
            stateTimer = 0;
            System.out.println("[AFK Mover] 检测到AFK，开始自动移动序列：前→后→左→右");
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
                    System.out.println("[AFK Mover] 自动移动序列完成，重新计时～");
                }
                default -> {}
            }
        }
        
        private void nextState(MoveState next) {
            currentState = next;
            stateTimer = 0;
            // 短暂暂停
            if (next != MoveState.COMPLETE) {
                // 暂停几个tick
            }
        }
    }
}