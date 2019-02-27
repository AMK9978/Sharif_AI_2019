package client;

import client.model.*;
import client.model.Map;
import java.util.*;

public class AI {
    private int pick_period = 0;

    private ArrayList<Cell> Blocked_Cells = new ArrayList<>();

    private ArrayList<Cell> blocks = new ArrayList<>();
    private boolean extraSentry = false;
    // to objective zone
    private HashMap<Hero, Integer> myHeroesHp = new HashMap<>();
    private PriorityQueue<NiceCell> niceCells = new PriorityQueue<>();

    //adding this class to have enough information in order to move
    class NiceCell implements Comparable<NiceCell> {
        double score = 0;
        Hero hero;
        Direction direction = Direction.DOWN;
        Cell target;
        String heroName = "";

        @Override
        public int compareTo(NiceCell niceCell) {
            if (this.score > niceCell.score) {
                return 1;
            } else {
                return -1;
            }
        }
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
        Cell minCell = null;
        int min = 9999;
        for (Cell cell : cells) {
            if (!isOccupied(cell, world) && !Blocked_Cells.contains(cell) && !blocks.contains(cell)) {
                int num = world.manhattanDistance(hero.getCurrentCell(), cell);
                if (num < min) {
                    min = num;
                    minCell = cell;
                }
            }
        }
        if (minCell != null) {
            closestCells.add(minCell);
        }
        return closestCells;
    }
    private ArrayList<Cell> getClosestCellsForDodge(World world, Hero hero,int dodge_board) {
        Cell[] cells = world.getMap().getObjectiveZone();
        ArrayList<Cell> closestCells = new ArrayList<>();
        Cell minCell = null;
        for (Cell cell : cells) {
            if (!isOccupied(cell, world) && !Blocked_Cells.contains(cell) && !blocks.contains(cell)
                    && world.manhattanDistance(hero.getCurrentCell(),cell) <= dodge_board ) {
                minCell = cell;
            }
        }
        if (minCell != null) {
            closestCells.add(minCell);
        }else{
            Random random = new Random();
            closestCells.add(world.getMap().getObjectiveZone()[Math.abs(random.nextInt()%20)]);
        }
        return closestCells;
    }

    //finding the best cell to move
    private void finding_good_cell_to_move(Hero[] My_heroes, Hero hero, World world) {
        boolean isSentry = false, isBlaster = false, isHealer = false, isGuardian = false;
        Hero Healer = null;
        for (Hero hero1 : My_heroes) {
            if (hero1.getName().equals(HeroName.HEALER)) {
                Healer = hero1;
                break;
            }
        }
        Hero intended_hero = null;
        double max;
        Direction direction = Direction.UP;
        String heroName = "";
        double score = 0;
        Cell cell;
        int niceTargetRow = 0;
        int niceTargetColl = 0;

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
        Cell targetCell = cell;

        //dont move
        if (targetCell.isInObjectiveZone()) {
            score = getCellScore(hero, world, targetCell, null, Healer, isHealer);
            score += 2 / hero.getMoveAPCost();
            if (max < score) {
                max = score;
                niceTargetRow = cell.getRow();
                niceTargetColl = cell.getColumn();
                intended_hero = hero;
                direction = null;
            }
        }

        //moving up
        if (world.getMap().isInMap(cell.getRow() - 1, cell.getColumn()) &&
                !world.getMap().getCell(cell.getRow() - 1, cell.getColumn()).isWall()) {

            targetCell = world.getMap().getCell(cell.getRow() - 1, cell.getColumn());

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

            targetCell = world.getMap().getCell(cell.getRow() + 1, cell.getColumn());
            score = getCellScore(hero, world, targetCell, Direction.DOWN, Healer, isHealer);
            Cell goodCell = getClosestCells(world, hero).get(0);
            Direction direction1[] = world.getPathMoveDirections(hero.getCurrentCell(), goodCell, Blocked_Cells);
            if (world.getCurrentTurn() == 4 && (hero.getName().equals(HeroName.SENTRY) || hero.getName().equals(HeroName.BLASTER))) {
                System.err.println("hero goes to good cell," + hero.getName() + ",good row:" + hero.getCurrentCell().getRow() + ",column:" + goodCell.getColumn()
                        + ",hero row:" + hero.getCurrentCell().getRow() + ",column:" + hero.getCurrentCell().getColumn());
                for (int i = 0; i < direction1.length; i++) {
                    System.out.println("direction i:" + i + "," + direction1[i]);
                }
            }
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

            targetCell = world.getMap().getCell(cell.getRow(), cell.getColumn() - 1);
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

            targetCell = world.getMap().getCell(cell.getRow(), cell.getColumn() + 1);
            score = getCellScore(hero, world, targetCell, Direction.RIGHT, Healer, isHealer);

            if (max < score) {
                max = score;
                niceTargetRow = cell.getRow();
                niceTargetColl = cell.getColumn() + 1;
                direction = Direction.RIGHT;
                intended_hero = world.getMyHero(cell);
            }
        }
        System.out.println("in the for:hero name:" + heroName + ",score:" + score);


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

    private double getCellScore(Hero hero, World world, Cell targetCell, Direction direction, Hero Healer,
                                Boolean isHealer) {
        double score = 0;
        Cell cell = hero.getCurrentCell();
        Hero[] oppHeroes = world.getOppHeroes();
        HeroName name = hero.getName();
        if (targetCell.isWall()) {
            return -100;
        }
        if (direction != null && Blocked_Cells.contains(targetCell)) {
            return -100;
        }
        if (targetCell.isInObjectiveZone())
            score += 1.5;
        Cell goodCell = getClosestCells(world, hero).get(0);
        System.out.println("good row:" + goodCell.getRow() + ",column:" + goodCell.getColumn());
        for (int i = 0; i < 5; i++) {
            if (world.getPathMoveDirections(cell, goodCell, Blocked_Cells).length == 0) {
                blocks.add(goodCell);
                goodCell = getClosestCells(world, hero).get(0);
            }
        }
        blocks.clear();
        if (!hero.getCurrentCell().isInObjectiveZone() && world.getPathMoveDirections(cell, goodCell, Blocked_Cells).length != 0 &&
                world.getPathMoveDirections(cell, goodCell, Blocked_Cells)[0] == direction) {
            score += 3;
            if (world.getCurrentTurn() == 4 && (name.equals(HeroName.SENTRY) || name.equals(HeroName.BLASTER))) {
                System.err.println("hero goes to good cell," + name
                        + ",hero row:" + hero.getCurrentCell().getRow() + ",column:" + hero.getCurrentCell().getColumn());
            }
        }
//        if (!targetCell.isInObjectiveZone()){
//            score += (5 - world.manhattanDistance(goodCell,targetCell))/4;
//        }
        if ((name.equals(HeroName.BLASTER) && hero.getAbility(AbilityName.BLASTER_BOMB).isReady()) ||
                name.equals(HeroName.HEALER)) {
            Cell[] objective = world.getMap().getObjectiveZone();
//            for (Cell aCell : objective) {
            Boolean notVis = true;
            for (Hero oppHeroe : oppHeroes) {
                if (world.isInVision(oppHeroe.getCurrentCell(), targetCell)) {
                    notVis = false;
                    break;
                }
            }
            if (notVis) {
//                    if (world.getPathMoveDirections(cell, targetCell).length != 0 &&
//                            direction == world.getPathMoveDirections(cell, aCell)[0])
                score += 1;
//                    break;
            }
//            }
        } else if (name.equals(HeroName.SENTRY)) {
            for (Hero oHero : oppHeroes) {
                if (oHero.getName() == HeroName.SENTRY) {
                    continue;
                }
                Cell heroCell = oHero.getCurrentCell();
                if (world.isInVision(heroCell, targetCell)) {
                    if (hero.getAbility(AbilityName.SENTRY_RAY).isReady()) {
                        score += 2;
                        break;
                    }
                    if (world.manhattanDistance(heroCell, targetCell)
                            <= hero.getAbility(AbilityName.SENTRY_ATTACK).getRange()) {
                        score += 1;
                        break;
                    }
                } else {
                    if (world.manhattanDistance(heroCell, targetCell)
                            <= hero.getAbility(AbilityName.SENTRY_ATTACK).getRange()) {
                        score += 0.5;
                        break;
                    }
                }

            }

        } else if (name.equals(HeroName.GUARDIAN)) {
            for (Hero oHero : oppHeroes) {
                Cell heroCell = oHero.getCurrentCell();
                if (world.isInVision(heroCell, targetCell)) {
                    score += (3 - world.manhattanDistance(heroCell, targetCell));
                }

            }
        }

        if (Healer != null) {

            Cell healerCell = Healer.getCurrentCell();
            if (!isHealer && world.manhattanDistance(cell, healerCell) <= 4)
                score += .5;
        }
        System.out.println("hero :" + hero.getName() + ",score:" + score);

        for (Hero oppHero : oppHeroes) {
            Cell oppCell = oppHero.getCurrentCell();
            if (!oppHero.getCurrentCell().isInVision())
                continue;
            if ((hero.getName() != HeroName.SENTRY || (hero.getName().equals(HeroName.SENTRY) &&
                    !hero.getAbility(AbilityName.SENTRY_RAY).isReady()))
                    && oppHero.getName().equals(HeroName.SENTRY) && world.isInVision(hero.getCurrentCell()
                    , targetCell)) {
                score -= 1;
            }
            if (hero.getName().equals(HeroName.BLASTER) &&
                    !hero.getAbility(AbilityName.BLASTER_BOMB).isReady()) {
                if (world.manhattanDistance(targetCell, oppCell) > 4 &&
                        world.manhattanDistance(targetCell, oppCell) < 8
                        && world.isInVision(hero.getCurrentCell(), oppCell)) {
                    score += 1;
                }

            }
            if (oppHero.getName().equals(HeroName.GUARDIAN)) {

                if (world.manhattanDistance(targetCell, oppCell) < 3) {
                    score -= (3 - world.manhattanDistance(targetCell, oppCell)) * 2;
                    System.out.println("Near of Guardian");
                }
            }
            if (oppHero.getName().equals(HeroName.HEALER)) {
                if (world.manhattanDistance(targetCell, oppCell) < 4)
                    score -= 1;
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
//        if (counter != 0) {
//            ave_row = Math.round(sum_rows / counter);
//            ave_col = Math.round(sum_cols / counter);
//            score -= world.manhattanDistance(world.getMap().getCell(ave_row, ave_col), targetCell) * (-0.2);
//        }
        System.out.println("hero :" + hero.getName() + ",score:" + score + ",direction:" + direction);
        System.out.println();
        if (name.equals(HeroName.BLASTER)) {
            System.out.println();
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
        System.out.println(world.getMap().getMyRespawnZone()[0].getColumn() + "," + world.getMap().getMyRespawnZone()[0].getRow());
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
            System.out.println("turn: " + world.getCurrentTurn());
            finding_good_cell_to_move(heroes, My_hero, world);
            NiceCell niceCell;
            niceCell = niceCells.poll();

            System.out.println("hero in nice cell: " + niceCell.hero + ",and its score:" + niceCell.score);
            System.out.println("direction for hero to move in nice cell: " + niceCell.direction);
            System.out.println("blocked size: " + Blocked_Cells.size());
            if (niceCell.direction != null) {
                world.moveHero(niceCell.hero, niceCell.direction);
                if (!Arrays.asList(world.getMyDeadHeroes()).contains(My_hero)
                        && world.getAP() >= niceCell.hero.getMoveAPCost()) {
                    Blocked_Cells.remove(niceCell.hero.getCurrentCell());
                    Blocked_Cells.add(niceCell.target);
                }
            }
            for (int j = 0; j < Blocked_Cells.size(); j++) {
                System.out.println("blocked cell row:" + Blocked_Cells.get(j).getRow() + ",column:" + Blocked_Cells.get(j).getColumn());
            }
            niceCells.clear();
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
            boolean flag = true;
            Cell hero_cell = hero.getCurrentCell();
            for (int i = 0; i < world.getOppHeroes().length; i++) {
                if (world.getOppHeroes()[i].getName().equals(HeroName.GUARDIAN)
                        && world.getOppHeroes()[i].getCurrentHP() > 70
                        && world.manhattanDistance(hero.getCurrentCell(),
                        world.getOppHeroes()[i].getCurrentCell()) < 3) {
                    // go to dodge and escape!
                    flag = false;
                }
            }
            if (flag) {
                if (hero.getName().equals(HeroName.SENTRY)) {

                    if (hero.getAbility(AbilityName.SENTRY_RAY).isReady()) {
                        int minHP = 1000;
                        Hero goodOpp = null;
                        for (Hero oppHero : Opp_Heroes) {
                            if (!oppHero.getCurrentCell().isInVision())
                                continue;
                            if (world.isInVision(hero_cell, oppHero.getCurrentCell())) {
                                if (oppHero.getCurrentHP() < minHP) {
                                    minHP = oppHero.getCurrentHP();
                                    goodOpp = oppHero;
                                }
                            }
                        }
                        if (goodOpp != null)
                            world.castAbility(hero, AbilityName.SENTRY_RAY, goodOpp.getCurrentCell());
                    } else {
                        int minHP = 1000;
                        Hero goodOpp = null;
                        for (Hero oppHero : Opp_Heroes) {
                            if (!oppHero.getCurrentCell().isInVision())
                                continue;
                            if (hero.getAbility(AbilityName.SENTRY_ATTACK).isReady()
                                    && world.isInVision(hero_cell, oppHero.getCurrentCell())) {
                                if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell())
                                        <= hero.getAbility(AbilityName.SENTRY_ATTACK).getRange()
                                        + hero.getAbility(AbilityName.SENTRY_ATTACK).getAreaOfEffect()) {

                                    if (oppHero.getCurrentHP() < minHP) {
                                        minHP = oppHero.getCurrentHP();
                                        goodOpp = oppHero;
                                    }
                                }
                            }
                        }
                        if (goodOpp != null)
                            world.castAbility(hero, AbilityName.SENTRY_ATTACK, goodOpp.getCurrentCell());
                    }

                } else if (hero.getName().equals(HeroName.BLASTER)) {
                    if (hero.getAbility(AbilityName.BLASTER_BOMB).isReady()) {
                        int minHP = 1000;
                        Hero goodOpp = null;
                        for (Hero oppHero : Opp_Heroes) {
                            if (!oppHero.getCurrentCell().isInVision())
                                continue;
                            if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell())
                                    - hero.getAbility(AbilityName.BLASTER_BOMB).getAreaOfEffect() <=
                                    hero.getAbility(AbilityName.BLASTER_BOMB).getRange()) {

                                if (oppHero.getCurrentHP() < minHP) {
                                    minHP = oppHero.getCurrentHP();
                                    goodOpp = oppHero;
                                }
                            }
                        }
                        if (goodOpp != null)
                            world.castAbility(hero, AbilityName.BLASTER_BOMB, goodOpp.getCurrentCell());
                    } else if (hero.getAbility(AbilityName.BLASTER_ATTACK).isReady()) {
                        int minHP = 1000;
                        Hero goodOpp = null;
                        for (Hero oppHero : Opp_Heroes) {
                            if (!oppHero.getCurrentCell().isInVision())
                                continue;
                            if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell())
                                    - hero.getAbility(AbilityName.BLASTER_ATTACK).getAreaOfEffect() <=
                                    hero.getAbility(AbilityName.BLASTER_ATTACK).getRange()
                                    && world.isInVision(hero_cell, oppHero.getCurrentCell())) {

                                if (oppHero.getCurrentHP() < minHP) {
                                    minHP = oppHero.getCurrentHP();
                                    goodOpp = oppHero;
                                }
                            }
                        }
                        if (goodOpp != null)
                            world.castAbility(hero, AbilityName.BLASTER_ATTACK, goodOpp.getCurrentCell());
                    }
                } else if (hero.getName().equals(HeroName.GUARDIAN)) {
                    if (hero.getAbility(AbilityName.GUARDIAN_ATTACK).isReady()) {
                        int minHP = 1000;
                        Hero goodOpp = null;
                        for (Hero oppHero : Opp_Heroes) {
                            if (!oppHero.getCurrentCell().isInVision())
                                continue;
                            if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell())
                                    - hero.getAbility(AbilityName.GUARDIAN_ATTACK).getAreaOfEffect() <=
                                    hero.getAbility(AbilityName.GUARDIAN_ATTACK).getRange()
                                    && world.isInVision(hero_cell, oppHero.getCurrentCell())) {

                                if (oppHero.getCurrentHP() < minHP) {
                                    minHP = oppHero.getCurrentHP();
                                    goodOpp = oppHero;
                                }
                            }
                        }
                        if (goodOpp != null)
                            world.castAbility(hero, AbilityName.GUARDIAN_ATTACK, goodOpp.getCurrentCell());
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
                        int minHP = 1000;
                        Hero goodOpp = null;
                        for (Hero oppHero : Opp_Heroes) {
                            if (!oppHero.getCurrentCell().isInVision())
                                continue;
                            if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell())
                                    - hero.getAbility(AbilityName.HEALER_ATTACK).getAreaOfEffect() <=
                                    hero.getAbility(AbilityName.HEALER_ATTACK).getRange()) {

                                if (oppHero.getCurrentHP() < minHP) {
                                    minHP = oppHero.getCurrentHP();
                                    goodOpp = oppHero;
                                }
                            }
                        }
                        if (goodOpp != null)
                            world.castAbility(hero, AbilityName.HEALER_ATTACK, goodOpp.getCurrentCell());
                    }
                    if (hero.getAbility(AbilityName.HEALER_HEAL).isReady()) {
                        int minHP = 100;
                        Hero injury = null;
                        for (Hero hero1 : world.getMyHeroes()) {
                            if (!hero1.getCurrentCell().isInVision())
                                continue;
                            if (world.manhattanDistance(hero_cell, hero1.getCurrentCell())
                                    - hero.getAbility(AbilityName.HEALER_HEAL).getAreaOfEffect() <=
                                    hero.getAbility(AbilityName.HEALER_HEAL).getRange()
                                    && world.isInVision(hero_cell, hero1.getCurrentCell())) {

                                if (hero1.getCurrentHP() < minHP) {
                                    minHP = hero1.getCurrentHP();
                                    injury = hero1;
                                }
                            }
                        }
                        if (injury != null)
                            world.castAbility(hero, AbilityName.HEALER_HEAL, injury.getCurrentCell());
                    }
                }

            }
            //dodge
            if (!flag || (!hero.getCurrentCell().isInObjectiveZone() && world.getAP() > 0)) {
                if (!flag) {
                    if (hero.getName().equals(HeroName.SENTRY)) {
                        // 3
                        PriorityQueue<NiceCell> priorityQueue = new PriorityQueue<>();
                        for (int i = 0; i < 31; i++) {
                            for (int j = 0; j < 31; j++) {
                                if (!isOccupied(world.getMap().getCell(i, j), world) &&
                                        world.manhattanDistance(world.getMap().getCell(i, j), hero_cell) == 3) {
                                    NiceCell niceCell = new NiceCell();
                                    niceCell.score = getCellScore(hero, world, world.getMap().getCell(i, j),
                                            null, null, false);
                                    niceCell.direction = null;
                                    niceCell.hero = hero;
                                    niceCell.heroName = HeroName.SENTRY.toString();
                                    niceCell.target = world.getMap().getCell(i, j);
                                    priorityQueue.add(niceCell);
                                }
                            }
                        }
                        world.castAbility(hero, AbilityName.SENTRY_DODGE, priorityQueue.poll().target);
                    }
                    if (hero.getName().equals(HeroName.BLASTER)) {
                        // 4
                        PriorityQueue<NiceCell> priorityQueue = new PriorityQueue<>();
                        for (int i = 0; i < 31; i++) {
                            for (int j = 0; j < 31; j++) {
                                if (!isOccupied(world.getMap().getCell(i, j), world) &&
                                        world.manhattanDistance(world.getMap().getCell(i, j), hero_cell) == 4) {
                                    NiceCell niceCell = new NiceCell();
                                    niceCell.score = getCellScore(hero, world, world.getMap().getCell(i, j),
                                            null, null, false);
                                    niceCell.direction = null;
                                    niceCell.hero = hero;
                                    niceCell.heroName = HeroName.BLASTER.toString();
                                    niceCell.target = world.getMap().getCell(i, j);
                                    priorityQueue.add(niceCell);
                                }
                            }
                        }
                        world.castAbility(hero, AbilityName.BLASTER_DODGE, priorityQueue.poll().target);
                    }
                    if (hero.getName().equals(HeroName.GUARDIAN)) {
                        // 2
                        PriorityQueue<NiceCell> priorityQueue = new PriorityQueue<>();
                        for (int i = 0; i < 31; i++) {
                            for (int j = 0; j < 31; j++) {
                                if (!isOccupied(world.getMap().getCell(i, j), world) &&
                                        world.manhattanDistance(world.getMap().getCell(i, j), hero_cell) == 2) {
                                    NiceCell niceCell = new NiceCell();
                                    niceCell.score = getCellScore(hero, world, world.getMap().getCell(i, j),
                                            null, null, false);
                                    niceCell.direction = null;
                                    niceCell.hero = hero;
                                    niceCell.heroName = HeroName.GUARDIAN.toString();
                                    niceCell.target = world.getMap().getCell(i, j);
                                    priorityQueue.add(niceCell);
                                }
                            }
                        }
                        world.castAbility(hero, AbilityName.GUARDIAN_DODGE, priorityQueue.poll().target);
                    }
                    if (hero.getName().equals(HeroName.HEALER)) {
                        // 4
                        PriorityQueue<NiceCell> priorityQueue = new PriorityQueue<>();
                        for (int i = 0; i < 31; i++) {
                            for (int j = 0; j < 31; j++) {
                                if (!isOccupied(world.getMap().getCell(i, j), world) &&
                                        world.manhattanDistance(world.getMap().getCell(i, j), hero_cell) == 4) {
                                    NiceCell niceCell = new NiceCell();
                                    niceCell.score = getCellScore(hero, world, world.getMap().getCell(i, j),
                                            null, null, false);
                                    niceCell.direction = null;
                                    niceCell.hero = hero;
                                    niceCell.heroName = HeroName.HEALER.toString();
                                    niceCell.target = world.getMap().getCell(i, j);
                                    priorityQueue.add(niceCell);
                                }
                            }
                        }
                        world.castAbility(hero, AbilityName.HEALER_DODGE, priorityQueue.poll().target);
                    }
                } else {
                    if (hero.getName().equals(HeroName.SENTRY)) {
                        // 3
                        Cell des = getClosestCellsForDodge(world,hero,3).get(0);
                        world.castAbility(hero, AbilityName.SENTRY_DODGE, des);
                    }
                    if (hero.getName().equals(HeroName.BLASTER)) {
                        // 4
                        Cell des = getClosestCellsForDodge(world,hero,4).get(0);
                        world.castAbility(hero, AbilityName.BLASTER_DODGE, des);
                    }
                    if (hero.getName().equals(HeroName.GUARDIAN)) {
                        // 2
                        Cell des = getClosestCellsForDodge(world,hero,2).get(0);
                        world.castAbility(hero, AbilityName.GUARDIAN_DODGE, des);
                    }
                    if (hero.getName().equals(HeroName.HEALER)) {
                        // 4
                        Cell des = getClosestCellsForDodge(world,hero,4).get(0);
                        world.castAbility(hero, AbilityName.HEALER_DODGE, des);
                    }

                }
            }

        }
        System.out.println("My score:" + world.getMyScore());
        System.out.println("Opp score:" + world.getOppScore());
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

    private Boolean isOccupied(Cell cell, World world) {
        for (int i = 0; i < world.getMyHeroes().length; i++) {
            if (world.getMyHeroes()[i].getCurrentHP() >= 0) {
                if (world.getMyHeroes()[i].getCurrentCell().equals(cell)) {
                    return true;
                }
            }
        }
        return false;
    }
}
