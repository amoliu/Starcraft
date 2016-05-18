package eisbw.percepts.perceivers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import jnibwapi.JNIBWAPI;
import jnibwapi.Player;
import jnibwapi.Position;
import jnibwapi.Position.Positions;
import jnibwapi.Unit;
import jnibwapi.types.RaceType.RaceTypes;
import jnibwapi.types.TechType;
import jnibwapi.types.UnitType;
import jnibwapi.types.UpgradeType.UpgradeTypes;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.LinkedList;
import java.util.List;

public class BuildingPerceiverTest {

  private BuildingPerceiver perciever;
  @Mock
  private Unit unit;
  @Mock
  private UnitType unitType;
  @Mock
  private JNIBWAPI api;
  @Mock
  private Player self;

  /**
   * Initialize mocks.
   */
  @Before
  public void start() {
    MockitoAnnotations.initMocks(this);

    when(unit.getRallyPosition()).thenReturn(new Position(1, 1));
    when(unit.getRallyUnit()).thenReturn(unit);
    when(unit.getID()).thenReturn(1);
    when(unit.getTrainingQueueSize()).thenReturn(1);
    when(unit.isUpgrading()).thenReturn(true);
    when(unit.getUpgrade()).thenReturn(UpgradeTypes.Adrenal_Glands);
    when(api.getSelf()).thenReturn(self);
    when(self.isResearched(any(TechType.class))).thenReturn(false);
    when(unit.getType()).thenReturn(unitType);
    when(unitType.getName()).thenReturn("name");
    when(unitType.getSpaceProvided()).thenReturn(1);
    when(unit.getLoadedUnits()).thenReturn(new LinkedList<Unit>());

    perciever = new BuildingPerceiver(api, unit);
  }

  @Test
  public void size_test() {
    assertEquals(5, perciever.perceive().size());
    when(unit.getRallyPosition()).thenReturn(Positions.None);
    assertEquals(4, perciever.perceive().size());
    when(unit.getRallyUnit()).thenReturn(null);
    assertEquals(3, perciever.perceive().size());
    when(unit.isUpgrading()).thenReturn(false);
    assertEquals(2, perciever.perceive().size());
    List<Unit> loadedunits = new LinkedList<>();
    loadedunits.add(unit);
    when(unit.getLoadedUnits()).thenReturn(loadedunits);
    assertEquals(3, perciever.perceive().size());
    when(self.isResearched(any(TechType.class))).thenReturn(true);
    assertTrue(perciever.perceive().size() > 3);
  }
  
  @Test
  public void conditions_test() {
    when(unit.isLifted()).thenReturn(true);
    when(unit.getAddon()).thenReturn(unit);
    assertEquals(0, perciever.getConditions().size());
    when(unitType.getRaceID()).thenReturn(RaceTypes.Terran.getID());
    assertEquals(2, perciever.getConditions().size());
    when(unit.isLifted()).thenReturn(false);
    when(unit.getAddon()).thenReturn(null);
    assertEquals(0, perciever.getConditions().size());
  }

}