package utils;

import entities.Player;

import java.io.IOException;

public class NameReader implements Runnable {
    private final Player player;

    public NameReader(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        try {
            player.setName(player.getReader().readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
