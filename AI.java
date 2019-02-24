package client;

import client.model.*;
import client.model.Map;

import java.util.*;

public class AI {
    private Random random = new Random();
    private int pick_period = 0;
    Boolean b = false;

    private ArrayList<Cell> Blocked_Cells = new ArrayList<>();
    private boolean extraSentry = false;
    // to objective zone
    private HashMap<Hero, Integer> myHeroesHp = new HashMap<>();
    private PriorityQueue<NiceCell> niceCells = new PriorityQueue<>();

    //adding this class to have enough information in order to move
    class NiceCell {
        double score = 0;
        Hero hero;
        Direction direction = Direction.DOWN;
        Cell target;
        String heroName = "";
    }


    private double Variance(ArrayList<Integer> dis) {
        double var = 0, ave = 0, sum2 = 0;
        int sum1 = 0;

        for (Integer di : dis) {
            sum1 += di;
        }

        ave = sum1 / dis.size();

        for (Integer di : dis) {
            sum2 += Math.pow(di - ave, 2);
        }

        var = sum2 / dis.size();
        return var;
    }

    private ArrayList<Cell> getClosestCells(World world, Hero hero) {
        Cell[] cells = world.getMap().getObjectiveZone();
        ArrayList<Cell> closestCells = new ArrayList<>();
//        ArrayList<Cell>cellArrayList = new ArrayList<>();
        Cell minCell = null;
        int min = 9999;
        for (Cell cell : cells) {
            int i;
            for (i = 0; i < world.getMyHeroes().length; i++) {
                if (world.getMyHeroes()[i].getCurrentCell().equals(cell)) {
                    break;
                }
            }
            if (i == world.getMyHeroes().length) {
                int num = world.manhattanDistance(hero.getCurrentCell(), cell);
//                cellArrayList.add(cell);
                if (num < min) {
                    min = num;
                    minCell = cell;
                }
            }
        }
        if (minCell != null) {
            closestCells.add(minCell);
        }
//        if (closestCells.size()!=0){
        return closestCells;
//        }else{
//            return cellArrayList;
//        }
    }

    //finding the best cell to move
    private void finding_good_cell_to_move(Hero[] My_heroes, World world) {
        boolean isSentry = false, isBlaster = false, isHealer = false, isGuardian = false;
        Hero Healer = null;
        for (Hero hero : My_heroes) {
            if (hero.getName().equals(HeroName.HEALER)) {
                Healer = hero;
                break;
            }
        }
        Hero intended_hero = null;
        double max = -9999;
        Direction direction = Direction.UP;
        String heroName = "";
        double score;
        Cell cell;
        int niceTargetRow = 0;
        int niceTargetColl = 0;

        for (Hero hero : My_heroes) {
            max = -9999;

            if (hero.getName().equals(HeroName.SENTRY)) {
                isSentry = true;
                isBlaster = false;
                isHealer = false;
                isGuardian = false;
            } else if (hero.getName().equals(HeroName.BLASTER)) {
                isBlaster = true;
                isSentry = false;
                isHealer = false;
                isGuardian = false;
            } else if (hero.getName().equals(HeroName.HEALER)) {
                isHealer = true;
                isSentry = false;
                isBlaster = false;
                isGuardian = false;
            } else if (hero.getName().equals(HeroName.GUARDIAN)) {
                isGuardian = true;
                isSentry = false;
                isBlaster = false;
                isHealer = false;
            }

            cell = hero.getCurrentCell();

            //moving up
            if (world.getMap().isInMap(cell.getRow() - 1, cell.getColumn()) &&
                    !world.getMap().getCell(cell.getRow() - 1, cell.getColumn()).isWall()) {

                Cell targetCell = world.getMap().getCell(cell.getRow() - 1, cell.getColumn());

                score = getCellScore(hero, world, targetCell, Direction.UP, Healer, isHealer);
                if (max < score) {
                    max = score;
                    niceTargetRow = cell.getRow() - 1;
                    niceTargetColl = cell.getColumn();
                    intended_hero = world.getMyHero(cell);
                    direction = Direction.UP;
                }
            }

            //moving down
            if (world.getMap().isInMap(cell.getRow() + 1, cell.getColumn()) &&
                    !world.getMap().getCell(cell.getRow() + 1, cell.getColumn()).isWall()) {

                Cell targetCell = world.getMap().getCell(cell.getRow() + 1, cell.getColumn());
                score = getCellScore(hero, world, targetCell, Direction.DOWN, Healer, isHealer);

                if (max < score) {
                    max = score;
                    niceTargetRow = cell.getRow() + 1;
                    niceTargetColl = cell.getColumn();
                    direction = Direction.DOWN;
                    intended_hero = world.getMyHero(cell);
                }
            }

            //moving left
            if (world.getMap().isInMap(cell.getRow(), cell.getColumn() - 1) &&
                    !world.getMap().getCell(cell.getRow(), cell.getColumn() - 1).isWall()) {

                Cell targetCell = world.getMap().getCell(cell.getRow(), cell.getColumn() - 1);
                score = getCellScore(hero, world, targetCell, Direction.LEFT, Healer, isHealer);

                if (max < score) {
                    max = score;
                    niceTargetRow = cell.getRow();
                    niceTargetColl = cell.getColumn() - 1;
                    direction = Direction.LEFT;
                    intended_hero = world.getMyHero(cell);
                }
            }

            //moving right
            if (world.getMap().isInMap(cell.getRow(), cell.getColumn() + 1) &&
                    !world.getMap().getCell(cell.getRow(), cell.getColumn() + 1).isWall()) {

                Cell targetCell = world.getMap().getCell(cell.getRow(), cell.getColumn() + 1);
                score = getCellScore(hero, world, targetCell, Direction.RIGHT, Healer, isHealer);

                if (max < score) {
                    max = score;
                    niceTargetRow = cell.getRow();
                    niceTargetColl = cell.getColumn() + 1;
                    direction = Direction.RIGHT;
                    intended_hero = world.getMyHero(cell);
                }
            }
        }

        if (isSentry) {
            heroName = "sentry";
        } else if (isBlaster) {
            heroName = "blaster";
        } else if (isHealer) {
            heroName = "healer";
        } else if (isGuardian) {
            heroName = "guardian";
        }

        NiceCell nice_cell = new NiceCell();
        nice_cell.score = max;
        nice_cell.hero = intended_hero;
        nice_cell.target = world.getMap().getCell(niceTargetRow, niceTargetColl);
        nice_cell.heroName = heroName;
        nice_cell.direction = direction;
        niceCells.add(nice_cell);
    }

    private double getCellScore(Hero hero, World world, Cell targetCell, Direction direction, Hero Healer, Boolean isHealer) {
        double score = 0;
        Cell cell = hero.getCurrentCell();
        Hero[] oppHeroes = world.getOppHeroes();
        HeroName name = hero.getName();
        if (targetCell.isInObjectiveZone())
            score += 1;
        Cell goodCell = getClosestCells(world, hero).get(0);
        if (world.getPathMoveDirections(cell, goodCell)[0] == direction)
            score += 2;
        if ((name == HeroName.BLASTER && hero.getAbility(AbilityName.BLASTER_BOMB).isReady()) || name == HeroName.HEALER) {
            Cell[] objective = world.getMap().getObjectiveZone();
            for (Cell aCell : objective) {
                Boolean notVis = true;
                for (Hero oppHeroe : oppHeroes) {
                    if (world.isInVision(oppHeroe.getCurrentCell(), aCell)) {
                        notVis = false;
                        break;
                    }
                }
                if (notVis) {
                    if (world.getPathMoveDirections(cell, aCell).length != 0 &&
                            direction == world.getPathMoveDirections(cell, aCell)[0])
                        score += 2;
                    break;
                }
            }
        } else if (name == HeroName.SENTRY) {
            for (Hero oHero : oppHeroes) {
                if (oHero.getName() == HeroName.SENTRY) {
                    continue;
                }
                Cell heroCell = oHero.getCurrentCell();
                if (world.isInVision(heroCell, targetCell)) {
                    if (hero.getAbility(AbilityName.SENTRY_RAY).isReady())
                        score += 2;
                    if (world.manhattanDistance(heroCell, targetCell)
                            <= hero.getAbility(AbilityName.SENTRY_ATTACK).getRange())
                        score += 1;

                } else {
                    if (world.manhattanDistance(heroCell, targetCell)
                            <= hero.getAbility(AbilityName.SENTRY_ATTACK).getRange())
                        score += 0.5;
                }

            }

        }
        if (Healer != null) {

            Cell healerCell = Healer.getCurrentCell();
            if (!isHealer && world.manhattanDistance(cell, healerCell) <= 4)
                score += .5;
        }

        //if (!myHeroesHp.isEmpty())
        //   if (hero.getCurrentHP() - myHeroesHp.get(hero) < 0)
        //       score -= 2;

        for (Hero oppHero : oppHeroes) {
            Cell oppCell = oppHero.getCurrentCell();
            if (!oppHero.getCurrentCell().isInVision())
                continue;
            if ((hero.getName() != HeroName.SENTRY || !hero.getAbility(AbilityName.SENTRY_RAY).isReady())
                    && oppHero.getName().equals(HeroName.SENTRY) && world.isInVision(hero.getCurrentCell()
                    , targetCell)) {
                score -= 1;
            } else if (oppHero.getName().equals(HeroName.BLASTER)
                    && (hero.getName() != HeroName.BLASTER || !hero.getAbility(AbilityName.BLASTER_BOMB).isReady())) {
                if (world.manhattanDistance(targetCell, oppCell) > 4)
                    score += 1;
            } else if (oppHero.getName().equals(HeroName.GUARDIAN)) {

                if (world.manhattanDistance(targetCell, oppCell) > 1)
                    score += 1;
            } else if (oppHero.getName().equals(HeroName.HEALER)) {
                if (world.manhattanDistance(targetCell, oppCell) > 4)
                    score += 1;
            }
        }
        int counter = 0;
        int ave_row = 0, ave_col = 0;
        int sum_rows = 0, sum_cols = 0;
        for (int i = 0; i < world.getMyHeroes().length; i++) {
            if (world.getMyHeroes()[i].getCurrentHP() > 0) {
                sum_cols += world.getMyHeroes()[i].getCurrentCell().getColumn();
                sum_rows += world.getMyHeroes()[i].getCurrentCell().getRow();
                counter++;
            }
        }
        if (counter != 0) {
            ave_row = Math.round(sum_rows / counter);
            ave_col = Math.round(sum_cols / counter);
            score -= world.manhattanDistance(world.getMap().getCell(ave_row, ave_col), targetCell) * (-0.2);
        }
        return score;
    }

    public void preProcess(World world) {
        System.out.println("pre process started");
        Map map = world.getMap();
        Cell[][] cells = map.getCells();
        ArrayList<Integer> dis = new ArrayList<>();

        for (int i = 0; i < 31; i++) {
            for (int j = 0; j < 31; j++) {
                if (cells[i][j].isWall())
                    dis.add(world.manhattanDistance(i, j, 0, 0));
            }
        }

        int delta = 250;

        int dispersion = (int) Variance(dis);
        System.out.println("dispersion:" + dispersion);
        if (dispersion > delta) {
            extraSentry = true;
        }
//        closestCells = getClosestCells(world);

    }

    public void pickTurn(World world) {

        System.out.println("pick turn!:" + pick_period);
        if (pick_period == 0) {
            world.pickHero(HeroName.SENTRY);
            pick_period++;
        } else if (pick_period == 1) {
            world.pickHero(HeroName.BLASTER);
            pick_period++;
        } else if (pick_period == 2) {
            world.pickHero(HeroName.HEALER);
            pick_period++;
        } else {
            if (extraSentry) {
                world.pickHero(HeroName.SENTRY);
            } else {
                world.pickHero(HeroName.BLASTER);
            }
            pick_period = 0;
        }
    }


    public void moveTurn(World world) {
        System.out.println("move started");
        Hero My_hero;
        Hero[] heroes = world.getMyHeroes();
        System.out.println("Phase in move:" + world.getMovePhaseNum());
        Blocked_Cells.clear();
        for (int j = 0; j < world.getMyHeroes().length; j++) {
            Blocked_Cells.add(world.getMyHeroes()[j].getCurrentCell());
        }
        for (int i = 0; i < 4; i++) {
            My_hero = world.getMyHeroes()[i];
            Cell target = getClosestCells(world, My_hero).get(0);
            if (My_hero.getCurrentCell().isInObjectiveZone() && !myHeroesHp.isEmpty()) {

                if (My_hero.getCurrentHP() == myHeroesHp.get(My_hero))
                    continue;
                else if (My_hero.getCurrentHP() - myHeroesHp.get(My_hero) < 0) {
                    finding_good_cell_to_move(heroes, world);
                    NiceCell niceCell = new NiceCell();
                    niceCell = niceCells.peek();
                    System.out.println("size of niceCells is: " + niceCells.size());
                    System.out.println("here in nice cell: " + niceCell.hero);
                    System.out.println("direction for hero to move in nice cell: " + niceCell.direction);
                    System.out.println("turn:" + world.getCurrentTurn() + ",id:" + My_hero.getId() +
                            ",row:" + niceCell.target.getRow() + ",col:" + niceCell.target.getColumn());
                    world.moveHero(niceCell.hero, niceCell.direction);
                    if (!Arrays.asList(world.getMyDeadHeroes()).contains(My_hero)
                            && world.getAP() >= My_hero.getMoveAPCost()) {
                        Blocked_Cells.remove(My_hero.getCurrentCell());
                        Blocked_Cells.add(niceCell.target);
                    }
                    niceCells.clear();
                }
            } else {
                My_hero = world.getMyHeroes()[i];
                Cell origin = My_hero.getCurrentCell();
                Direction directions[] =
                        world.getPathMoveDirections(origin, target, Blocked_Cells);
                System.out.println("Blocked_Cells size:" + Blocked_Cells.size());
                if (directions.length == 0) {
                    System.out.println("continue and id is :" + My_hero.getId() + ",turn:" + world.getCurrentTurn()
                            + ",target row:" + target.getRow() + ",col:" + target.getColumn());
                    continue;
                }
                Direction direction = directions[0];
                System.out.println("turn:" + world.getCurrentTurn() + ",id:" + My_hero.getId() +
                        ",row:" + My_hero.getCurrentCell().getRow() + ",col:" + My_hero.getCurrentCell().getColumn());

                System.out.println("MyHero column before:" + My_hero.getCurrentCell().getColumn() +
                        ",row:" + My_hero.getCurrentCell().getRow());
                world.moveHero(My_hero, direction);
                System.out.println("MyHero column after:" + My_hero.getCurrentCell().getColumn() +
                        ",row:" + My_hero.getCurrentCell().getRow());
                if (!Arrays.asList(world.getMyDeadHeroes()).contains(My_hero) &&
                        world.getAP() >= My_hero.getMoveAPCost()) {
                    Blocked_Cells.remove(My_hero.getCurrentCell());
                    Blocked_Cells.add(change(direction, world, origin.getRow(), origin.getColumn()));
                }
            }
            myHeroesHp.put(My_hero, My_hero.getCurrentHP());
        }
        Blocked_Cells.clear();
    }


    public void actionTurn(World world) {
        System.out.println("action started");
        Hero[] heroes = world.getMyHeroes();
        ArrayList<Cell> Opp_cells = new ArrayList<>();
        Hero[] Opp_Heroes = world.getOppHeroes();

        for (int i = 0; i < 4; i++) {
            if (Opp_Heroes[i].getCurrentCell().isInVision()) {
                Opp_cells.add(Opp_Heroes[i].getCurrentCell());
            }
        }

        for (Hero hero : heroes) {
            Cell hero_cell = hero.getCurrentCell();
            if (hero.getName().equals(HeroName.SENTRY)) {

                if (hero.getAbility(AbilityName.SENTRY_RAY).isReady()) {
                    int minHP = 100;
                    Hero goodOpp = null;
                    for (Hero oppHero : Opp_Heroes) {
                        if (!oppHero.getCurrentCell().isInVision())
                            continue;
                        if (world.isInVision(hero_cell, oppHero.getCurrentCell())) {
                            goodOpp = oppHero;
                            if (oppHero.getCurrentHP() < minHP) {
                                break;
                            }
                        }
                    }
                    if (goodOpp != null)
                        world.castAbility(hero, AbilityName.SENTRY_RAY, goodOpp.getCurrentCell());
                } else {
                    int minHP = 100;
                    Hero goodOpp = null;
                    for (Hero oppHero : Opp_Heroes) {
                        if (!oppHero.getCurrentCell().isInVision())
                            continue;
                        if (hero.getAbility(AbilityName.SENTRY_ATTACK).isReady()
                                && world.isInVision(hero_cell, oppHero.getCurrentCell())) {
                            if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell())
                                    <= hero.getAbility(AbilityName.SENTRY_ATTACK).getRange()
                                    + hero.getAbility(AbilityName.SENTRY_ATTACK).getAreaOfEffect()) {
                                goodOpp = oppHero;
                                if (oppHero.getCurrentHP() < minHP) {
                                    break;
                                }
                            }
                        }
                    }
                    if (goodOpp != null)
                        world.castAbility(hero, AbilityName.SENTRY_ATTACK, goodOpp.getCurrentCell());
                }

            } else if (hero.getName().equals(HeroName.BLASTER)) {
                if (hero.getAbility(AbilityName.BLASTER_BOMB).isReady()) {
                    int minHP = 100;
                    Hero goodOpp = null;
                    for (Hero oppHero : Opp_Heroes) {
                        if (!oppHero.getCurrentCell().isInVision())
                            continue;
                        if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell())
                                - hero.getAbility(AbilityName.BLASTER_BOMB).getAreaOfEffect() <=
                                hero.getAbility(AbilityName.BLASTER_BOMB).getRange()) {
                            goodOpp = oppHero;
                            if (oppHero.getCurrentHP() < minHP) {
                                break;
                            }
                        }
                    }
                    if (goodOpp != null)
                        world.castAbility(hero, AbilityName.BLASTER_BOMB, goodOpp.getCurrentCell());
                } else if (hero.getAbility(AbilityName.BLASTER_ATTACK).isReady()) {
                    int minHP = 100;
                    Hero goodOpp = null;
                    for (Hero oppHero : Opp_Heroes) {
                        if (!oppHero.getCurrentCell().isInVision())
                            continue;
                        if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell())
                                - hero.getAbility(AbilityName.BLASTER_ATTACK).getAreaOfEffect() <=
                                hero.getAbility(AbilityName.BLASTER_ATTACK).getRange()) {
                            goodOpp = oppHero;
                            if (oppHero.getCurrentHP() < minHP) {
                                break;
                            }
                        }
                    }
                    if (goodOpp != null)
                        world.castAbility(hero, AbilityName.BLASTER_ATTACK, goodOpp.getCurrentCell());
                }
            } else if (hero.getName().equals(HeroName.GUARDIAN)) {
                if (hero.getAbility(AbilityName.GUARDIAN_ATTACK).isReady()) {
                    for (Cell Opp_cell : Opp_cells) {
                        if (!Opp_cell.isInVision()) {
                            continue;
                        }
                        if (world.manhattanDistance(hero_cell, Opp_cell) <=
                                hero.getAbility(AbilityName.GUARDIAN_ATTACK).getRange()) {
                            world.castAbility(hero, AbilityName.GUARDIAN_ATTACK, Opp_cell);
                        }
                    }
                }
                if (hero.getAbility(AbilityName.GUARDIAN_FORTIFY).isReady()) {
                    for (Hero hero1 : world.getMyHeroes()) {
                        Cell des = hero1.getCurrentCell();
                        if (hero1.getCurrentHP() < hero1.getMaxHP() && world.manhattanDistance(hero_cell, des) <=
                                hero.getAbility(AbilityName.GUARDIAN_FORTIFY).getRange()) {
                            world.castAbility(hero, AbilityName.GUARDIAN_FORTIFY, des);
                        }
                    }
                }
            } else {
                // Healer
                if (hero.getAbility(AbilityName.HEALER_ATTACK).isReady()) {
                    for (Cell Opp_cell : Opp_cells) {
                        if (!Opp_cell.isInVision())
                            continue;
                        if (world.manhattanDistance(hero_cell, Opp_cell) <=
                                hero.getAbility(AbilityName.HEALER_ATTACK).getRange()) {
                            world.castAbility(hero, AbilityName.HEALER_ATTACK, Opp_cell);
                        }
                    }
                }
                if (hero.getAbility(AbilityName.HEALER_HEAL).isReady()) {
                    for (Hero hero1 : world.getMyHeroes()) {
                        Cell des = hero1.getCurrentCell();
                        if (hero1.getCurrentHP() < hero1.getMaxHP() && world.manhattanDistance(hero_cell, des) <=
                                hero.getAbility(AbilityName.HEALER_HEAL).getRange()) {
                            world.castAbility(hero, AbilityName.HEALER_HEAL, des);
                        }
                    }
                }
            }


            //dodge
            if (!hero.getCurrentCell().isInObjectiveZone() && world.getAP() > 0) {
                Cell Objective_cell = world.getMap().getObjectiveZone()[random.nextInt(5)];
                int row = Objective_cell.getRow(), column = Objective_cell.getColumn();
                Direction direction[] = world.getPathMoveDirections(hero_cell, Objective_cell);
                if (direction.length == 0) {
                    continue;
                }
                ArrayList<Cell> targets = new ArrayList<>();
                targets.add(change(direction[0], world, row, column));
                if (hero.getName().equals(HeroName.SENTRY)) {
                    // 3
                    for (int i = 1; i < direction.length && i < 3; i++) {
                        targets.add(change(direction[i], world, targets.get(targets.size() - 1).getRow()
                                , targets.get(targets.size() - 1).getColumn()));
                    }
                    world.castAbility(hero, AbilityName.SENTRY_DODGE, targets.get(targets.size() - 1));
                }
                if (hero.getName().equals(HeroName.BLASTER)) {
                    // 4
                    for (int i = 1; i < direction.length && i < 4; i++) {
                        targets.add(change(direction[i], world, targets.get(targets.size() - 1).getRow()
                                , targets.get(targets.size() - 1).getColumn()));
                    }
                    world.castAbility(hero, AbilityName.BLASTER_DODGE, targets.get(targets.size() - 1));
                }
                if (hero.getName().equals(HeroName.GUARDIAN)) {
                    // 2
                    for (int i = 1; i < direction.length && i < 2; i++) {
                        targets.add(change(direction[i], world, targets.get(targets.size() - 1).getRow()
                                , targets.get(targets.size() - 1).getColumn()));
                    }
                    world.castAbility(hero, AbilityName.GUARDIAN_DODGE, targets.get(targets.size() - 1));
                }
                if (hero.getName().equals(HeroName.HEALER)) {
                    // 4
                    for (int i = 1; i < direction.length && i < 4; i++) {
                        targets.add(change(direction[i], world, targets.get(targets.size() - 1).getRow()
                                , targets.get(targets.size() - 1).getColumn()));
                    }
                    world.castAbility(hero, AbilityName.HEALER_DODGE, targets.get(targets.size() - 1));
                }
            }

        }
    }

    private Cell change(Direction direction, World world, int row, int column) {
        Cell target;
        if (direction.equals(Direction.UP)) {
            target = world.getMap().getCell(row - 1, column);
        } else if (direction.equals(Direction.DOWN)) {
            target = world.getMap().getCell(row + 1, column);
        } else if (direction.equals(Direction.LEFT)) {
            target = world.getMap().getCell(row, column - 1);
        } else {
            target = world.getMap().getCell(row, column + 1);
        }
        return target;
    }

}
