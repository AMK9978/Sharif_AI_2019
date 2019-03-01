package client;

import client.model.*;
import client.model.Map;

import java.util.*;


public class AI {
    private int pick_period = 0;

    private ArrayList<Cell> Blocked_Cells = new ArrayList<>();

    private HashMap<Hero, Integer> hero_turn_pair = new HashMap<>();
    private HashMap<Hero, Cell> hero_cell_pair = new HashMap<>();

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

    private Cell getClosestWalls(World world, Hero hero) {
        int min = 9999;
        Cell hero_cell = hero.getCurrentCell();
        Cell most_suspicious = null;
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                if (world.getMap().isInMap(hero_cell.getRow() + i, hero_cell.getColumn() + j)) {
                    if (world.getMap().getCell(hero_cell.getRow() + i, hero_cell.getColumn() + j).isWall()) {
                        Cell suspicious = world.getMap().getCell
                                (hero_cell.getRow() + i, hero_cell.getColumn() + j);
                        if (world.manhattanDistance(suspicious, world.getMap().getOppRespawnZone()[0]) < min) {
                            min = world.manhattanDistance(suspicious, world.getMap().getOppRespawnZone()[0]);
                            if (hero_cell.getColumn() >= suspicious.getColumn() &&
                                    hero_cell.getRow() >= suspicious.getRow()) {
                                //----h
                                //-w---
                                //*----
                                most_suspicious = world.getMap().getCell(suspicious.getRow() + 1,
                                        suspicious.getColumn() - 1);

                            } else if (hero_cell.getColumn() < suspicious.getColumn() &&
                                    hero_cell.getRow() > suspicious.getRow()) {
                                //-----*
                                //---w-
                                //h----
                                most_suspicious = world.getMap().getCell(suspicious.getRow() - 1, suspicious.getColumn() + 1);


                            } else if (hero_cell.getColumn() < suspicious.getColumn() &&
                                    hero_cell.getRow() < suspicious.getRow()) {
                                //*----
                                //--w--
                                //----h
                                most_suspicious = world.getMap().getCell(suspicious.getRow() + 1, suspicious.getColumn() + 1);

                            } else {
                                most_suspicious = world.getMap().getCell(suspicious.getRow() - 1, suspicious.getColumn() - 1);
                            }
                        }
                    }
                }
            }
        }
        return most_suspicious;
    }

    private Cell getClosestCellsForDodge(World world, Hero hero, int dodge_board) {
        Cell[] cells = world.getMap().getObjectiveZone();
        Cell minCell = null;
        for (Cell cell : cells) {
            if (!isOccupied(cell, world) && !Blocked_Cells.contains(cell) && !blocks.contains(cell)
                    && world.manhattanDistance(hero.getCurrentCell(), cell) <= dodge_board) {
                minCell = cell;
            }
        }
        return minCell;
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
            score += 3 / hero.getMoveAPCost();

            if (hero_cell_pair.containsKey(hero) &&
                    hero_turn_pair.get(hero) >= 2) {
                score -= 1.5;
                hero_cell_pair.remove(hero);
                hero_turn_pair.remove(hero);
            }
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
        {
            if (targetCell.isWall()) {
                return -100;
            }
            if (direction != null && Blocked_Cells.contains(targetCell)) {
                return -100;
            }
            Cell cell = hero.getCurrentCell();
            Hero[] oppHeroes = world.getOppHeroes();
            HeroName name = hero.getName();
            Hero[] myHeroes = world.getMyHeroes();
            int powerRange = 0, attackRange = 0, dodgeRange;
            Boolean powerReady = false, attackReady = false;
            Ability powerAbility = null, attackAbility = null;
            if (name.equals(HeroName.BLASTER)) {
                powerAbility = hero.getAbility(AbilityName.BLASTER_BOMB);
                powerReady = powerAbility.isReady();
                if (powerReady)
                    powerRange = powerAbility.getRange();

                attackAbility = hero.getAbility(AbilityName.BLASTER_ATTACK);
                attackReady = attackAbility.isReady();
                if (attackReady)
                    attackRange = attackAbility.getRange();
                dodgeRange = hero.getAbility(AbilityName.BLASTER_DODGE).getRange();
            } else if (name.equals(HeroName.SENTRY)) {
                powerAbility = hero.getAbility(AbilityName.SENTRY_RAY);
                powerReady = powerAbility.isReady();
                if (powerReady)
                    powerRange = powerAbility.getRange();

                attackAbility = hero.getAbility(AbilityName.SENTRY_ATTACK);
                attackReady = attackAbility.isReady();
                if (attackReady)
                    attackRange = attackAbility.getRange();
                dodgeRange = hero.getAbility(AbilityName.SENTRY_DODGE).getRange();
            } else if (name.equals(HeroName.HEALER)) {
                powerAbility = hero.getAbility(AbilityName.HEALER_HEAL);
                powerReady = powerAbility.isReady();
                if (powerReady)
                    powerRange = powerAbility.getRange();

                attackAbility = hero.getAbility(AbilityName.HEALER_ATTACK);
                attackReady = attackAbility.isReady();
                if (attackReady)
                    attackRange = attackAbility.getRange();

                dodgeRange = hero.getAbility(AbilityName.HEALER_DODGE).getRange();
            } else {
                powerAbility = hero.getAbility(AbilityName.GUARDIAN_FORTIFY);
                powerReady = powerAbility.isReady();
                if (powerReady)
                    powerRange = powerAbility.getRange();

                attackAbility = hero.getAbility(AbilityName.GUARDIAN_ATTACK);
                attackReady = attackAbility.isReady();
                if (attackReady)
                    attackRange = attackAbility.getRange();

                dodgeRange = hero.getAbility(AbilityName.GUARDIAN_DODGE).getRange();
            }

            Cell[] objective = world.getMap().getObjectiveZone();
            int enteha = 0, ebteda = 0;
            for (Cell aCell : objective) {
                if (aCell.getColumn() > enteha) {
                    if (enteha == 0) {
                        ebteda = aCell.getColumn();
                    }
                    enteha = aCell.getColumn();
                } else {
                    break;
                }
            }

//            if ((name.equals(HeroName.BLASTER) && powerReady) || name.equals(HeroName.HEALER)) {
//                if (powerReady || attackReady) {
//                    Cell goodCell = null;
//                    int attack = 0, power = 0;
//                    for (int i = ebteda - 2; i <= enteha + 2; i++) {
//                        for (int j = ebteda - 2; j <= enteha + 2; j++) {
//                            Cell aCell = world.getMap().getCell(i, j);
//
//
//                            int enemyScore = 0;
//                            for (Hero oppHeroe : oppHeroes) {
//                                Cell hCell = oppHeroe.getCurrentCell();
//                                power = 0;
//                                attack = 0;
//                                if (!world.isInVision(hCell, aCell)) {
//                                    if (powerReady &&
//                                            world.manhattanDistance(hCell, aCell) < powerRange
//                                                    + powerAbility.getAreaOfEffect())
//                                        power++;
//                                    else if (attackReady && world.manhattanDistance(hCell, aCell) < attackRange)
//                                        attack++;
//
//                                } else {
//
//                                    //some code
//                                }
//
//                            }
//                            if (power + attack > 0) {
//                                if ((power * 2) + (attack * 2) > enemyScore) {
//                                    enemyScore = (power * 2) + (attack * 2);
//                                    goodCell = aCell;
//                                }
//                            }
//                        }
//                    }
//                }
//
//            } else
            if (name.equals(HeroName.GUARDIAN)) {
                int minHP = 1000;
                Hero tHero = null;
                if (attackReady) {
                    Boolean motamarkez = false;
                    for (Hero dHero : oppHeroes) {
                        if (dHero.getCurrentHP() == 0)
                            continue;
                        boolean flag = true;
                        if (dHero.getCurrentHP() < minHP) {
                            if (world.manhattanDistance(dHero.getCurrentCell(), hero.getCurrentCell()) > 2) {
                                int minimun_dis = 99;
                                for (int i = 0; i < 4; i++) {
                                    if (world.getOppHeroes()[i].getCurrentHP() > 0) {
                                        if (world.manhattanDistance(hero.getCurrentCell(), world.getOppHeroes()[i].
                                                getCurrentCell()) < world.manhattanDistance(dHero.getCurrentCell(),
                                                hero.getCurrentCell())) {
                                            if (world.getOppHeroes()[i].getCurrentCell().isInObjectiveZone()) {
                                                motamarkez = true;
                                                tHero = world.getOppHeroes()[i];
                                                flag = false;
                                                dHero = tHero;
                                            }
                                        }
                                    }
                                }
                            }

                            if (flag) {
                                minHP = dHero.getCurrentHP();
                                tHero = dHero;
                                motamarkez = true;
                            }
                        }
                    }
                    if (motamarkez) {
                        if (world.getPathMoveDirections(cell, tHero.getCurrentCell(),Blocked_Cells).length > 0) {
                            if (direction == world.getPathMoveDirections(cell, tHero.getCurrentCell(),Blocked_Cells)[0])
                                score += 3;
                        }

                    } else {
                        for (Hero dHero : oppHeroes) {
                            if (dHero.getCurrentHP() > 0) {
                                if (dHero.getCurrentHP() < minHP) {
                                    minHP = dHero.getCurrentHP();
                                    tHero = dHero;
                                }

                            }
                        }
                        if (tHero != null) {
                            if (world.getPathMoveDirections(cell, tHero.getCurrentCell(), Blocked_Cells).length > 0) {
                                if (direction == world.getPathMoveDirections(cell, tHero.getCurrentCell(), Blocked_Cells)[0])
                                    score += 3;
                            }
                        }
                    }

                } else if (powerReady) {
                    minHP = 1000;
                    Hero komak = null;
                    for (Hero myHero : myHeroes) {
                        if (myHero.getCurrentHP() < minHP &&
                                world.manhattanDistance(cell, myHero.getCurrentCell()) < 6)
                            komak = myHero;

                    }
                    if (komak != null) {
                        if (world.getPathMoveDirections(cell, komak.getCurrentCell(),Blocked_Cells).length > 0) {
                            if (world.getPathMoveDirections(cell, komak.getCurrentCell(),Blocked_Cells)[0] == direction)
                                score += 2;
                        }
//                        else if (direction == null)
//                            score += 3;
                    }
                }else {
                    if (direction == null)
                            score += 3;
                }
            }
        }
        Cell cell = hero.getCurrentCell();
        Hero[] oppHeroes = world.getOppHeroes();
        HeroName name = hero.getName();
        if (targetCell.isWall()) {
            return -100;
        }
        if (direction != null && Blocked_Cells.contains(targetCell)) {
            return -100;
        }
        if (name.equals(HeroName.SENTRY)) {
            if (targetCell.isInObjectiveZone())
                score += 1;
            Cell goodCell = getClosestCells(world, hero).get(0);
            System.out.println("good row:" + goodCell.getRow() + ",column:" + goodCell.getColumn());
            for (int i = 0; i < 5; i++) {
                if (world.getPathMoveDirections(cell, goodCell, Blocked_Cells).length == 0) {
                    blocks.add(goodCell);
                    goodCell = getClosestCells(world, hero).get(0);
                }
            }
            blocks.clear();
            if (!hero.getCurrentCell().isInObjectiveZone() && world.getPathMoveDirections(cell, goodCell, Blocked_Cells)
                    .length != 0 &&
                    world.getPathMoveDirections(cell, goodCell, Blocked_Cells)[0] == direction) {
                score += 2.5;
            }
        } else {
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
            if (!hero.getCurrentCell().isInObjectiveZone() && world.getPathMoveDirections(cell, goodCell, Blocked_Cells)
                    .length != 0 &&
                    world.getPathMoveDirections(cell, goodCell, Blocked_Cells)[0] == direction) {
                score += 2.5;
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
            for (Hero oppHero : oppHeroes) {
                if (world.isInVision(oppHero.getCurrentCell(), targetCell)) {
                    notVis = false;
                    break;
                }
            }
            if (notVis) {
//                    if (world.getPathMoveDirections(cell, targetCell).length != 0 &&
//                            direction == world.getPathMoveDirections(cell, aCell)[0])
                score += 2;
//                    break;
            }
//            }
        } else if (name.equals(HeroName.SENTRY)) {
            for (Hero oHero : oppHeroes) {
                if (oHero.getName() == HeroName.SENTRY &&
                        world.isInVision(oHero.getCurrentCell(), hero.getCurrentCell())
                        && oHero.getCurrentHP() > hero.getCurrentHP()) {
                    score -= 1;
                }
                Cell heroCell = oHero.getCurrentCell();
                if (world.isInVision(heroCell, targetCell)) {
                    if (hero.getAbility(AbilityName.SENTRY_RAY).isReady()) {
                        score += 2.5;
                        if (direction == null) {
                            score += 0.5;
                        }
                        break;
                    }
                    if (world.manhattanDistance(heroCell, targetCell)
                            <= hero.getAbility(AbilityName.SENTRY_ATTACK).getRange()) {
                        score += 1.5;
                        if (direction == null) {
                            score += 0.5;
                        }
                        break;
                    }
                }
            }
            for (Hero oHero : oppHeroes) {
                Cell heroCell = oHero.getCurrentCell();
                if (world.manhattanDistance(heroCell, hero.getCurrentCell()) <= 7 &&
                        oHero.getName().equals(HeroName.BLASTER)) {
//                    if (hero.getAbility(AbilityName.SENTRY_RAY).isReady()) {
                    score -= (7 - world.manhattanDistance(heroCell, targetCell));
//                    } else {
//                        score -= (7 - world.manhattanDistance(heroCell, targetCell)) / 1.5;
//                    }

                    break;
                }
            }

        } else if (name.equals(HeroName.GUARDIAN)) {
//            for (Hero oHero : oppHeroes) {
//                Cell heroCell = oHero.getCurrentCell();
//                if (world.isInVision(heroCell, targetCell)) {
//                    score += (3 - world.manhattanDistance(heroCell, targetCell));
//                }
//            }
        } else if (name.equals(HeroName.HEALER)) {
            for (Hero oHero : oppHeroes) {
                Cell heroCell = oHero.getCurrentCell();
                if (world.manhattanDistance(heroCell, hero.getCurrentCell()) <= 7 &&
                        oHero.getName().equals(HeroName.BLASTER) && oHero.getCurrentHP() > 100) {
                    score -= (7 - world.manhattanDistance(heroCell, targetCell)) / 2;
                    break;
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
            if ((!hero.getName().equals(HeroName.SENTRY) || (hero.getName().equals(HeroName.SENTRY) &&
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
            if (oppHero.getName().equals(HeroName.GUARDIAN) && !name.equals(HeroName.GUARDIAN)) {

                if (world.manhattanDistance(targetCell, oppCell) < 3) {
                    score -= (3 - world.manhattanDistance(targetCell, oppCell)) * 2;
                    System.out.println("Near of Guardian");
                }
            }
//            if (oppHero.getName().equals(HeroName.HEALER)) {
//                if (world.manhattanDistance(targetCell, oppCell) < 4 &&
//                        !Healer.getAbility(AbilityName.HEALER_ATTACK).isReady())
//                    score -= 1;
//            }

            if (oppHero.getName().equals(HeroName.BLASTER) && oppHero.getCurrentHP() > 50) {
                for (int i = 0; i < world.getMyHeroes().length; i++) {
                    if (!world.getMyHeroes()[i].equals(hero)) {
                        if (world.getMyHeroes()[i].getCurrentHP() > 0) {
                            if (world.manhattanDistance(targetCell, world.getMyHeroes()[i].getCurrentCell()) < 3) {
                                score -= 2 -
                                        world.manhattanDistance(targetCell, world.getMyHeroes()[i].getCurrentCell());
                            }
                        }
                    }
                }
            }
        }


//        int counter = 0;
//        int ave_row = 0, ave_col = 0;
//        int sum_rows = 0, sum_cols = 0;
//        for (int i = 0; i < world.getMyHeroes().length; i++) {
//            if (world.getMyHeroes()[i].getCurrentHP() > 0) {
//                sum_cols += world.getMyHeroes()[i].getCurrentCell().getColumn();
//                sum_rows += world.getMyHeroes()[i].getCurrentCell().getRow();
//                counter++;
//            }
//        }
//        if (counter != 0 && hero.getCurrentCell().isInObjectiveZone()) {
//            ave_row = Math.round(sum_rows / counter);
//            ave_col = Math.round(sum_cols / counter);
//            score -= world.manhattanDistance(world.getMap().getCell(ave_row, ave_col), targetCell) * (-0.2);
//        }
        System.out.println("hero :" + hero.getName() + ",score:" + score + ",direction:" + direction);
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
        for (int i = 0; i < 4; i++) {
            System.out.println("col:" + world.getMap().getMyRespawnZone()[i].getColumn()
                    + "," + world.getMap().getMyRespawnZone()[i].getRow());
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
            world.pickHero(HeroName.GUARDIAN);
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
        if (pick_period == 0) {
            for (int i = 0; i < 4; i++) {
                System.out.println("name:" + world.getMyHeroes()[i].getName() + ",id:" + world.getMyHeroes()[i].getId());
            }
            pick_period = 1;
        }
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
                System.out.println("current row:" + My_hero.getCurrentCell().getRow() + ",col:" +
                        My_hero.getCurrentCell().getColumn() + ",dir:" + niceCell.direction);
                System.out.println("row:" + niceCell.target.getRow() + ",column:" + niceCell.target.getColumn());
                if (!Arrays.asList(world.getMyDeadHeroes()).contains(My_hero)
                        && world.getAP() >= niceCell.hero.getMoveAPCost()) {
                    Blocked_Cells.remove(niceCell.hero.getCurrentCell());
                    Blocked_Cells.add(niceCell.target);
                }
            } else {

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
        Hero[] Opp_Heroes = world.getOppHeroes();
        ArrayList<Hero> target_heroes = new ArrayList<>();
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
                        if (goodOpp != null) {
                            world.castAbility(hero, AbilityName.SENTRY_RAY, goodOpp.getCurrentCell());
                            System.out.println("sentry raid and end:" + goodOpp.getCurrentCell());
                        } else if (world.getAP() >= 25) {
                            if (hero_cell_pair.get(hero) == null) {
                                hero_cell_pair.put(hero, hero_cell);
                                hero_turn_pair.put(hero, 1);
                            } else {
                                hero_turn_pair.put(hero, hero_turn_pair.get(hero) + 1);
                                hero_cell_pair.put(hero, hero_cell);
                            }
                        }
                    } else {
                        int minHP = 1000;
                        Hero goodOpp = null;
                        int cut = 0;
                        Hero occasion = null;
                        int min = 999;
                        for (Hero oppHero : Opp_Heroes) {
                            if (!oppHero.getCurrentCell().isInVision())
                                continue;
                            if (hero.getAbility(AbilityName.SENTRY_ATTACK).isReady()
                                    && world.isInVision(hero_cell, oppHero.getCurrentCell())) {
                                if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell())
                                        <= hero.getAbility(AbilityName.SENTRY_ATTACK).getRange()
                                        + hero.getAbility(AbilityName.SENTRY_ATTACK).getAreaOfEffect()) {
                                    if (oppHero.getCurrentHP() < minHP) {
                                        if (target_heroes.contains(goodOpp) && oppHero.getCurrentHP() <= 30) {
                                            cut = 1;
                                            min = oppHero.getCurrentHP();
                                            occasion = oppHero;
                                            continue;
                                        }
                                        if (min > oppHero.getCurrentHP()) {
                                            min = oppHero.getCurrentHP();
                                            occasion = goodOpp;
                                        }
                                        minHP = oppHero.getCurrentHP();
                                        goodOpp = oppHero;
                                    }
                                }
                            }
                        }
                        if (goodOpp != null) {
//                            if (occasion != null){
//                                world.castAbility(hero, AbilityName.SENTRY_ATTACK, occasion.getCurrentCell());
//                            }else{
                            world.castAbility(hero, AbilityName.SENTRY_ATTACK, goodOpp.getCurrentCell());
                            System.out.println("sentry attacked and end:" + goodOpp.getCurrentCell());
//                            }
                        } else if (world.getAP() >= 15) {
                            if (hero_cell_pair.get(hero) == null) {
                                hero_cell_pair.put(hero, hero_cell);
                                hero_turn_pair.put(hero, 1);
                            } else {
                                hero_turn_pair.put(hero, hero_turn_pair.get(hero) + 1);
                                hero_cell_pair.put(hero, hero_cell);
                            }
                        }
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
                        if (goodOpp != null) {
                            world.castAbility(hero, AbilityName.BLASTER_BOMB, goodOpp.getCurrentCell());
                            System.out.println("blaster bombed and end:" + goodOpp.getCurrentCell());
                        } else {
                            Cell suspicious = getClosestWalls(world, hero);
                            world.castAbility(hero, AbilityName.BLASTER_BOMB, suspicious);
                            System.out.println("blaster fortified and end:" + suspicious);
                            if (world.getAP() >= 25) {
                                if (hero_cell_pair.get(hero) == null) {
                                    hero_cell_pair.put(hero, hero_cell);
                                    hero_turn_pair.put(hero, 1);
                                } else {
                                    hero_turn_pair.put(hero, hero_turn_pair.get(hero) + 1);
                                    hero_cell_pair.put(hero, hero_cell);
                                }
                            }
                        }
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
                        if (goodOpp != null) {
                            world.castAbility(hero, AbilityName.BLASTER_ATTACK, goodOpp.getCurrentCell());
                            System.out.println("blaster attacked and end:" + goodOpp.getCurrentCell());
                        } else if (world.getAP() >= 15) {
                            if (hero_cell_pair.get(hero) == null) {
                                hero_cell_pair.put(hero, hero_cell);
                                hero_turn_pair.put(hero, 1);
                            } else {
                                hero_turn_pair.put(hero, hero_turn_pair.get(hero) + 1);
                                hero_cell_pair.put(hero, hero_cell);
                            }
                        }
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
                        if (goodOpp != null) {
                            world.castAbility(hero, AbilityName.GUARDIAN_ATTACK, goodOpp.getCurrentCell());
                            System.out.println("guardian fortified and end:" + goodOpp.getCurrentCell());
                        } else if (world.getAP() >= 15) {
                            if (hero_cell_pair.get(hero) == null) {
                                hero_cell_pair.put(hero, hero_cell);
                                hero_turn_pair.put(hero, 1);
                            } else {
                                hero_turn_pair.put(hero, hero_turn_pair.get(hero) + 1);
                                hero_cell_pair.put(hero, hero_cell);
                            }
                        }
                    }
                    if (hero.getAbility(AbilityName.GUARDIAN_FORTIFY).isReady()) {
                        for (Hero hero1 : world.getMyHeroes()) {
                            Cell des = hero1.getCurrentCell();
                            if (hero1.getCurrentHP() < hero1.getMaxHP() && world.manhattanDistance(hero_cell, des) <=
                                    hero.getAbility(AbilityName.GUARDIAN_FORTIFY).getRange()) {
                                world.castAbility(hero, AbilityName.GUARDIAN_FORTIFY, des);
                                System.out.println("guardian fortified and end:" + des);
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
                            if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell()) <=
                                    hero.getAbility(AbilityName.HEALER_ATTACK).getRange()) {

                                if (oppHero.getCurrentHP() < minHP) {
                                    minHP = oppHero.getCurrentHP();
                                    goodOpp = oppHero;
                                }
                            }
                        }
                        if (goodOpp != null) {
                            world.castAbility(hero, AbilityName.HEALER_ATTACK, goodOpp.getCurrentCell());
                            System.out.println("healer attacked and end:" + goodOpp.getCurrentCell());
                        } else if (world.getAP() >= 15) {
                            Cell suspicious = getClosestWalls(world, hero);
                            world.castAbility(hero, AbilityName.HEALER_ATTACK, suspicious);
                            System.out.println("healer attacked and end:" + suspicious);
                            if (hero_cell_pair.get(hero) == null) {
                                hero_cell_pair.put(hero, hero_cell);
                                hero_turn_pair.put(hero, 1);
                            } else {
                                hero_turn_pair.put(hero, hero_turn_pair.get(hero) + 1);
                                hero_cell_pair.put(hero, hero_cell);
                            }
                        }
                    }
                    if (hero.getAbility(AbilityName.HEALER_HEAL).isReady()) {
                        int minHP = 1000;
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
                        if (injury != null) {
                            world.castAbility(hero, AbilityName.HEALER_HEAL, injury.getCurrentCell());
                            System.out.println("healer healed , end:" + injury.getCurrentCell());
                        }
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
                                } else if (!isOccupied(world.getMap().getCell(i, j), world) &&
                                        world.manhattanDistance(world.getMap().getCell(i, j), hero_cell) == 2) {
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
                        System.out.println("sentry dodged");
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
                                } else if (!isOccupied(world.getMap().getCell(i, j), world) &&
                                        world.manhattanDistance(world.getMap().getCell(i, j), hero_cell) == 3) {
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
                        System.out.println("blaster dodged");
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
                                } else if (!isOccupied(world.getMap().getCell(i, j), world) &&
                                        world.manhattanDistance(world.getMap().getCell(i, j), hero_cell) == 1) {
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
                        System.out.println("guardian dodged");
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
                                } else if (!isOccupied(world.getMap().getCell(i, j), world) &&
                                        world.manhattanDistance(world.getMap().getCell(i, j), hero_cell) == 3) {
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
                        System.out.println("healer dodged");
                        world.castAbility(hero, AbilityName.HEALER_DODGE, priorityQueue.poll().target);
                    }
                } else {
                    if (hero.getName().equals(HeroName.SENTRY)) {
                        // 3
                        Cell des = getClosestCellsForDodge(world, hero, 3);
                        if (des == null) {
                            des = getClosestCellsForDodge(world, hero, 2);
                            if (des == null) {
                                Random random = new Random();
                                des = world.getMap().getObjectiveZone()[Math.abs(random.nextInt() % 20)];

                            }
                        }
                        System.out.println("sentry dodged");
                        world.castAbility(hero, AbilityName.SENTRY_DODGE, des);
                    }
                    if (hero.getName().equals(HeroName.BLASTER)) {
                        // 4
                        Cell des = getClosestCellsForDodge(world, hero, 4);
                        if (des == null) {
                            des = getClosestCellsForDodge(world, hero, 3);
                            if (des == null) {
                                Random random = new Random();
                                des = world.getMap().getObjectiveZone()[Math.abs(random.nextInt() % 20)];

                            }
                        }
                        System.out.println("blaster dodged");
                        world.castAbility(hero, AbilityName.BLASTER_DODGE, des);
                    }
                    if (hero.getName().equals(HeroName.GUARDIAN)) {
                        // 2
                        Cell des = getClosestCellsForDodge(world, hero, 2);
                        if (des == null) {
                            des = getClosestCellsForDodge(world, hero, 1);
                            if (des == null) {
                                Random random = new Random();
                                des = world.getMap().getObjectiveZone()[Math.abs(random.nextInt() % 20)];

                            }
                        }
                        world.castAbility(hero, AbilityName.GUARDIAN_DODGE, des);
                        System.out.println("guardian dodged");
                    }
                    if (hero.getName().equals(HeroName.HEALER)) {
                        // 4
                        Cell des = getClosestCellsForDodge(world, hero, 4);
                        if (des == null) {
                            des = getClosestCellsForDodge(world, hero, 3);
                            if (des == null) {
                                Random random = new Random();
                                des = world.getMap().getObjectiveZone()[Math.abs(random.nextInt() % 20)];

                            }
                        }
                        System.out.println("healer dodged");
                        world.castAbility(hero, AbilityName.HEALER_DODGE, des);
                    }

                }
            }

        }

        System.out.println("My score:" + world.getMyScore());
        System.out.println("Opp score:" + world.getOppScore());
        for (int i = 0; i < world.getMyCastAbilities().length; i++) {
            System.out.println(world.getMyCastAbilities()[i].getAbilityName() + ",and:" +
                    world.getMyCastAbilities()[i].getCasterId());
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