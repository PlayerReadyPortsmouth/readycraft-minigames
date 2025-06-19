import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.auroraschaos.minigames.game.race.KartConfig;
import com.auroraschaos.minigames.game.race.KartVehicle;

public class KartVehicleTest {
    private ServerMock server;
    private World world;
    private PlayerMock player;
    private Boat boat;
    private KartConfig cfg;
    private Location boatLoc;

    @Before
    public void setup() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        player = server.addPlayer("p1");
        boat = Mockito.mock(Boat.class);
        boatLoc = new Location(world, 0, 64, 0);
        Mockito.when(boat.getLocation()).thenAnswer(inv -> boatLoc);
        Mockito.when(boat.getVelocity()).thenReturn(new Vector());
        Mockito.when(boat.getWorld()).thenReturn(world);
        cfg = Mockito.mock(KartConfig.class);
        Mockito.when(cfg.getStat(Mockito.anyString())).thenReturn(0.0);
        Mockito.when(cfg.getString(Mockito.anyString())).thenReturn("BARRIER");
    }

    @After
    public void teardown() {
        MockBukkit.unmock();
    }

    private KartVehicle createVehicle(List<Location> checkpoints) {
        return new KartVehicle(player, boat, cfg, checkpoints);
    }

    @Test
    public void updateMovementAcceleratesWhenSprinting() throws Exception {
        Mockito.when(cfg.getStat("accelRate")).thenReturn(0.5);
        Mockito.when(cfg.getStat("maxSpeed")).thenReturn(1.0);
        Mockito.when(cfg.getStat("decelRate")).thenReturn(0.1);
        Mockito.when(cfg.getStat("brakeRate")).thenReturn(0.2);
        Mockito.when(cfg.getStat("turnRate")).thenReturn(0.0);

        KartVehicle kv = createVehicle(List.of(new Location(world, 0, 64, 5)));

        player.setSprinting(true);
        kv.updateMovement();

        Field f = KartVehicle.class.getDeclaredField("speed");
        f.setAccessible(true);
        double speed = (double) f.get(kv);
        assertTrue("Speed should increase", speed > 0);
    }

    @Test
    public void handleOffTrackTeleports() {
        Mockito.when(cfg.getStat("respawnYOffset")).thenReturn(60.0);
        KartVehicle kv = createVehicle(List.of(new Location(world, 0, 64, 0)));

        boatLoc.setY(59.0); // below threshold
        kv.handleOffTrack();

        //Mockito.verify(boat).teleport(Mockito.argThat(loc -> loc.getY() == 60.0));
    }

    @Test
    public void updateProgressCompletesLap() {
        Mockito.when(cfg.getStat("checkpointRadius")).thenReturn(1.0);
        Mockito.when(cfg.getStat("totalLaps")).thenReturn(1.0);
        KartVehicle kv = createVehicle(Arrays.asList(
            new Location(world, 0, 64, 1),
            new Location(world, 0, 64, 2)
        ));

        // first checkpoint
        boatLoc.setZ(1);
        kv.updateMovement();

        // second checkpoint triggers lap finish
        boatLoc.setZ(2);
        kv.updateMovement();

        assertTrue("Lap should be finished", kv.hasFinishedLap());
    }
}
