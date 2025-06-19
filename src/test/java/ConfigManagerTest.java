import static org.junit.Assert.*;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.config.ConfigManager;
import com.auroraschaos.minigames.config.SpleefConfig;

public class ConfigManagerTest {
    private ServerMock server;
    private MinigamesPlugin plugin;

    @Before
    public void setup() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MinigamesPlugin.class);
    }

    @After
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void loadAllLoadsSpleefConfig() throws Exception {
        ConfigManager cm = new ConfigManager(plugin);
        cm.loadAll();
        SpleefConfig sc = cm.getSpleefConfig();
        assertNotNull(sc);
        assertEquals(300, sc.getGameDuration());
    }
}
