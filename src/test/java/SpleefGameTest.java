import static org.junit.Assert.*;
import java.lang.reflect.*;
import java.util.*;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scoreboard.Score;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.arena.Arena;
import com.auroraschaos.minigames.game.GameMode;
import com.auroraschaos.minigames.game.GameManager;
import com.auroraschaos.minigames.game.SpleefGame;
import com.auroraschaos.minigames.scoreboard.ScoreboardManager;
import com.auroraschaos.minigames.util.CountdownTimer;
import com.auroraschaos.minigames.stats.StatsManager;
import com.auroraschaos.minigames.arena.ArenaService;
import com.auroraschaos.minigames.config.ConfigManager;
import com.auroraschaos.minigames.config.SpleefConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import com.sk89q.worldedit.math.BlockVector3;

public class SpleefGameTest {
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

                Field cfgField = MinigamesPlugin.class.getDeclaredField("configManager");
                cfgField.setAccessible(true);
                ConfigManager cm = Mockito.mock(ConfigManager.class);
                YamlConfiguration yml = new YamlConfiguration();
                SpleefConfig sc = SpleefConfig.from(yml);
                Mockito.when(cm.getSpleefConfig()).thenReturn(sc);
                cfgField.set(this, cm);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onDisable() {
        }
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
    public void startTeleportsPlayersAndSetsScoreboard() throws Exception {
        World world = server.addSimpleWorld("test");
        BlockVector3 origin = BlockVector3.at(0, 64, 0);
        Arena arena = new Arena("arena", world, origin, "", Collections.emptyMap(), 0L);

        PlayerMock p1 = server.addPlayer("p1");
        PlayerMock p2 = server.addPlayer("p2");
        List<org.bukkit.entity.Player> players = Arrays.asList(p1, p2);

        SpleefGame game = new SpleefGame("SPLEEF", GameMode.CLASSIC, plugin, arena, players);
        game.start();

        Location expected = new Location(world, origin.getX() + 0.5, origin.getY() + 10, origin.getZ() + 0.5);
        assertEquals(expected, p1.getLocation());
        assertEquals(expected, p2.getLocation());

        ScoreboardManager sbm = plugin.getScoreboardManager();
        Field boardsField = ScoreboardManager.class.getDeclaredField("arenaBoards");
        boardsField.setAccessible(true);
        Map<?, ?> boards = (Map<?, ?>) boardsField.get(sbm);
        Object arenaBoard = boards.get(game.getId());
        assertNotNull(arenaBoard);
        Field entriesField = arenaBoard.getClass().getDeclaredField("entries");
        entriesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Score> entries = (Map<String, Score>) entriesField.get(arenaBoard);
        Score score = entries.get("players_" + game.getId());
        assertNotNull(score);
        assertEquals("Players Left: 2", score.getEntry());
    }
}
