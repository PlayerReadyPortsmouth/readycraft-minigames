import static org.junit.Assert.*;
import java.lang.reflect.*;
import java.util.*;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.arena.Arena;
import com.auroraschaos.minigames.game.GameMode;
import com.auroraschaos.minigames.game.GameManager;
import com.auroraschaos.minigames.game.SkyWarsGame;
import com.auroraschaos.minigames.scoreboard.ScoreboardManager;
import com.auroraschaos.minigames.util.CountdownTimer;
import com.auroraschaos.minigames.stats.StatsManager;
import com.auroraschaos.minigames.arena.ArenaService;
import com.sk89q.worldedit.math.BlockVector3;

public class SkyWarsGameTest {
    private ServerMock server;
    private TestPlugin plugin;

    public static class TestPlugin extends MinigamesPlugin {
        @Override
        public void onEnable() {
            try {
                Field sbField = MinigamesPlugin.class.getDeclaredField("scoreboardManager");
                sbField.setAccessible(true);
                ScoreboardManager sbm = new ScoreboardManager();
                sbField.set(this, sbm);

                Field cdField = MinigamesPlugin.class.getDeclaredField("countdownTimer");
                cdField.setAccessible(true);
                CountdownTimer timer = new CountdownTimer(this, sbm);
                cdField.set(this, timer);

                Field gmField = MinigamesPlugin.class.getDeclaredField("gameManager");
                gmField.setAccessible(true);
                gmField.set(this, Mockito.mock(GameManager.class));

                Field statsField = MinigamesPlugin.class.getDeclaredField("statsManager");
                statsField.setAccessible(true);
                statsField.set(this, Mockito.mock(StatsManager.class));

                Field arenaServiceField = MinigamesPlugin.class.getDeclaredField("arenaService");
                arenaServiceField.setAccessible(true);
                arenaServiceField.set(this, Mockito.mock(ArenaService.class));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        @Override public void onDisable() {}
    }

    @Before
    public void setup() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(TestPlugin.class);
    }

    @After
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void assignSpawnsTeleportsPlayers() throws Exception {
        World world = server.addSimpleWorld("test");
        BlockVector3 origin = BlockVector3.at(0, 64, 0);
        Arena arena = new Arena("arena", world, origin, "", Collections.emptyMap(), 0L);

        PlayerMock p1 = server.addPlayer("p1");
        PlayerMock p2 = server.addPlayer("p2");
        List<org.bukkit.entity.Player> players = Arrays.asList(p1, p2);

        SkyWarsGame game = new SkyWarsGame(plugin, arena, "SKY_WARS", GameMode.CLASSIC, players);

        List<Location> spawns = Arrays.asList(
            new Location(world, 1, 65, 1),
            new Location(world, -1, 65, -1)
        );
        Field spawnField = SkyWarsGame.class.getDeclaredField("spawnLocations");
        spawnField.setAccessible(true);
        spawnField.set(game, spawns);

        Method m = SkyWarsGame.class.getDeclaredMethod("assignSpawnsAndTeleportPlayers");
        m.setAccessible(true);
        m.invoke(game);

        assertEquals(spawns.get(0), p1.getLocation());
        assertEquals(spawns.get(1), p2.getLocation());
    }
}
