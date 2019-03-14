package client;

import client.model.*;
import client.model.Map;

import java.util.*;


public class AI {
    private int pick_period = 0;

    private ArrayList<Cell> Blocked_Cells = new ArrayList<>();
    private ArrayList<OppDetails> oppDetailsList = new ArrayList<>();
    private HashMap<Hero, Integer> hero_turn_pair = new HashMap<>();
    private HashMap<Hero, Cell> hero_cell_pair = new HashMap<>();
    private Cell[][] heroCellArray = new Cell[6][4];
    private ArrayList<Cell> blocks = new ArrayList<>();
    private boolean extraSentry = false;
    // to objective zone
    private HashMap<Hero, Integer> myHeroesHp = new HashMap<>();
    private PriorityQueue<NiceCell> niceCells = new PriorityQueue<>();
    private ArrayList<CastAbility> oppCastAbilities = new ArrayList<>();

    class OppDetails {
        int hp = 0;
        int hero_id;
        int coolDown = 0;
        int occurred_turn;
        CastAbility castAbility;

        OppDetails(CastAbility ability, int occurred_turn) {
            this.castAbility = ability;
            this.occurred_turn = occurred_turn;
            hero_id = castAbility.getCasterId();
            if (ability.getAbilityName().equals(AbilityName.BLASTER_ATTACK)) {
                coolDown = occurred_turn + 1;
            } else if (ability.getAbilityName().equals(AbilityName.BLASTER_BOMB)) {
                coolDown = occurred_turn + 4;
            } else if (ability.getAbilityName().equals(AbilityName.GUARDIAN_ATTACK)) {
                coolDown = occurred_turn + 1;
            } else if (ability.getAbilityName().equals(AbilityName.GUARDIAN_FORTIFY)) {
                coolDown = occurred_turn + 7;
            } else if (ability.getAbilityName().equals(AbilityName.SENTRY_RAY)) {
                coolDown = occurred_turn + 5;
            } else if (ability.getAbilityName().equals(AbilityName.SENTRY_ATTACK)) {
                coolDown = occurred_turn + 1;
            } else if (ability.getAbilityName().equals(AbilityName.HEALER_ATTACK)) {
                coolDown = occurred_turn + 1;
            } else if (ability.getAbilityName().equals(AbilityName.HEALER_HEAL)) {
                coolDown = occurred_turn + 6;
            }
        }
    }

    private double getCellScore(Hero hero, World world, Cell targetCell, Direction direction, Hero Healer,
                                Boolean isHealer, int Ohoy) {
        if (hero.getCurrentHP() <= 0) {
            return -999;
        }
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
            attackReady = false;
            powerReady = false;
            if (name.equals(HeroName.BLASTER)) {
                for (Hero oHero : oppHeroes) {
                    if (!oHero.getCurrentCell().isInVision() || oHero.getCurrentHP() <= 0)
                        continue;
                    Cell heroCell = oHero.getCurrentCell();
                    if (direction != null || oHero.getName().equals(HeroName.GUARDIAN)) {
                        if (world.manhattanDistance(heroCell, targetCell) < 8) {
                            score -= (7 - world.manhattanDistance(heroCell, targetCell)) / 2;
                        }

                        if (world.manhattanDistance(heroCell, targetCell) < 5) {
                            score -= (4 - world.manhattanDistance(heroCell, targetCell)) / 2;
                        }
                    }
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
            for (Hero oHero : oppHeroes) {
                if (oHero.getName() == HeroName.SENTRY &&
                        world.isInVision(oHero.getCurrentCell(), targetCell)
                        && oHero.getCurrentHP() > hero.getCurrentHP()) {
                    score -= 1;
                }
                Cell heroCell = oHero.getCurrentCell();
                if (world.isInVision(heroCell, targetCell)) {
                    if (hero.getAbility(AbilityName.SENTRY_RAY).isReady()) {
                        score += 2.5;
                        if (direction == null) {
                            score += 0.75;
                        }
                        break;
                    }
                    if (world.manhattanDistance(heroCell, targetCell)
                            <= hero.getAbility(AbilityName.SENTRY_ATTACK).getRange()) {
                        score += 1.5;
                        if (direction == null) {
                            score += 0.75;
                        }
                        break;
                    }
                }
            }
            for (Hero oHero : oppHeroes) {
                Cell heroCell = oHero.getCurrentCell();
                if (!heroCell.isInVision()) {
                    continue;
                }
                if (!isOpppowerReady(world, oHero.getId()) && !isOppattackReady(world, oHero.getId())) {
                    continue;
                }
                if (world.manhattanDistance(heroCell, hero.getCurrentCell()) <= 5 &&
                        oHero.getName().equals(HeroName.BLASTER) && !name.equals(HeroName.GUARDIAN)) {
                    score -= (5 - world.manhattanDistance(heroCell, targetCell));
                }
            }
        } else {
            if (targetCell.isInObjectiveZone())
                score += 1.5;
            Cell goodCell = getClosestCells(world, hero).get(0);
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
        if ((name.equals(HeroName.BLASTER) && hero.getAbility(AbilityName.BLASTER_BOMB).isReady()) ||
                name.equals(HeroName.HEALER)) {
            Cell[] objective = world.getMap().getObjectiveZone();
            Boolean notVis = true;
            for (Hero oppHero : oppHeroes) {
                if (world.isInVision(oppHero.getCurrentCell(), targetCell)) {
                    notVis = false;
                    break;
                }
            }
            if (notVis && targetCell.isInObjectiveZone()) {
                score += 2;
            }
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
        System.out.println("hero :" + hero.getName() + ",score:" + score + ",direction:" + direction);

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

            if (oppHero.getName().equals(HeroName.SHADOW)) {
                for (int i = 0; i < world.getMyHeroes().length; i++) {
                    if (world.getMyHeroes()[i].getCurrentHP() == 0) {
                        continue;
                    }
                    if (world.isInVision(world.getMyHeroes()[i].getCurrentCell(), oppCell)) {
                        for (int j = 0; j < world.getMyHeroes().length; j++) {
                            if (world.getMyHeroes()[j].equals(world.getMyHeroes()[i])
                                    || world.getMyHeroes()[j].getCurrentHP() <= 0) {
                                continue;
                            }
                            if (1 == squareCollision(world.getMyHeroes()[i].getCurrentCell()
                                    , world.getMyHeroes()[j].getCurrentCell(), oppHero.getCurrentCell())) {
                                if (1 == squareCollision(targetCell
                                        , world.getMyHeroes()[j].getCurrentCell(), oppHero.getCurrentCell())) {
                                    score -= 3;
                                }
                            }

                        }
                    }
                }
            }

        }
        System.out.println("hero :" + hero.getName() + ",score:" + score + ",direction:" + direction);
        return score;
    }

    private int squareCollision(Cell startCell, Cell targetCell, Cell cell) {
        if (cell == null || targetCell == null || startCell == null) {
            return -1;
        }
        boolean hasNegative = false;
        boolean hasPositive = false;
        boolean hasZero = false;
        for (int row = 2 * cell.getRow(); row <= 2 * (cell.getRow() + 1); row += 2)
            for (int column = 2 * cell.getColumn(); column <= 2 * (cell.getColumn() + 1); column += 2) {
                int crossProduct = crossProduct(2 * startCell.getRow() + 1, 2 * startCell.getColumn() + 1,
                        2 * targetCell.getRow() + 1, 2 * targetCell.getColumn() + 1, row, column);
                if (crossProduct < 0) hasNegative = true;
                else if (crossProduct > 0) hasPositive = true;
                else hasZero = true;
            }
        if (hasNegative && hasPositive) return 1;
        if (hasZero) return 0;
        return -1;
    }

    private int crossProduct(int x1, int y1, int x2, int y2, int x3, int y3) {
        return (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1);
    }

    private HeroName getOppHeroName(World world, int id) {
        for (int i = 0; i < world.getOppHeroes().length; i++) {
            if (world.getOppHeroes()[i].getId() == id) {
                return world.getOppHeroes()[i].getName();
            }
        }
        return null;
    }

    private boolean isOpppowerReady(World world, int id) {
        for (int i = 0; i < oppDetailsList.size(); i++) {
            if (oppDetailsList.get(i).hero_id == id) {
                // if opp dead , Don't request that opp to this method and just delete anything that belong to him
                // when you find out he has been killed
                if (world.getCurrentTurn() > oppDetailsList.get(i).coolDown) {
                    oppDetailsList.remove(i);
                } else {
                    if (getOppHeroName(world, oppDetailsList.get(i).hero_id).equals(HeroName.BLASTER)) {
                        if (oppDetailsList.get(i).castAbility.getAbilityName().equals(AbilityName.BLASTER_BOMB)) {
                            return false;
                        }
                    } else if (getOppHeroName(world, oppDetailsList.get(i).hero_id).equals(HeroName.GUARDIAN)) {
                        if (oppDetailsList.get(i).castAbility.getAbilityName().equals(AbilityName.GUARDIAN_FORTIFY)) {
                            return false;
                        }
                    } else if (getOppHeroName(world, oppDetailsList.get(i).hero_id).equals(HeroName.SENTRY)) {
                        if (oppDetailsList.get(i).castAbility.getAbilityName().equals(AbilityName.SENTRY_RAY)) {
                            return false;
                        }
                    } else if (getOppHeroName(world, oppDetailsList.get(i).hero_id).equals(HeroName.HEALER)) {
                        if (oppDetailsList.get(i).castAbility.getAbilityName().equals(AbilityName.HEALER_HEAL)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isOppattackReady(World world, int id) {
        for (int i = 0; i < oppDetailsList.size(); i++) {
            if (oppDetailsList.get(i).hero_id == id) {
                if (world.getCurrentTurn() > oppDetailsList.get(i).coolDown) {
                    oppDetailsList.remove(oppDetailsList.get(i));
                } else {
                    if (getOppHeroName(world, oppDetailsList.get(i).hero_id).equals(HeroName.BLASTER)) {
                        if (oppDetailsList.get(i).castAbility.getAbilityName().equals(AbilityName.BLASTER_ATTACK)) {
                            return false;
                        }
                    } else if (getOppHeroName(world, oppDetailsList.get(i).hero_id).equals(HeroName.GUARDIAN)) {
                        if (oppDetailsList.get(i).castAbility.getAbilityName().equals(AbilityName.GUARDIAN_ATTACK)) {
                            return false;
                        }
                    } else if (getOppHeroName(world, oppDetailsList.get(i).hero_id).equals(HeroName.SENTRY)) {
                        if (oppDetailsList.get(i).castAbility.getAbilityName().equals(AbilityName.SENTRY_ATTACK)) {
                            return false;
                        }
                    } else if (getOppHeroName(world, oppDetailsList.get(i).hero_id).equals(HeroName.HEALER)) {
                        if (oppDetailsList.get(i).castAbility.getAbilityName().equals(AbilityName.HEALER_ATTACK)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    //adding this class to have enough information in order to move
    class NiceCell implements Comparable<NiceCell> {
        double score = 0;
        Hero hero;
        Direction direction = Direction.DOWN;
        Cell target;
        String heroName = "";

        @Override
        public int compareTo(NiceCell niceCell) {
            if (this.score < niceCell.score) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    private int faseleFromObjective(World world, Cell aCell) {
        Cell[] objective = world.getMap().getObjectiveZone();
        int fasele = 1000;
        for (Cell bCell : objective) {
            if (world.manhattanDistance(aCell, bCell) < fasele)
                fasele = world.manhattanDistance(aCell, bCell);
        }
        return fasele;
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

    private Cell getClosestZoneCells(World world, Cell source_cell) {
        Cell[] cells = world.getMap().getObjectiveZone();
        Cell closestCells = null;
        Cell minCell = null;
        int min = 9999;
        for (Cell cell : cells) {
            int num = world.manhattanDistance(source_cell, cell);
            if (num < min) {
                min = num;
                minCell = cell;
            }
        }
        if (minCell != null) {
            closestCells = minCell;
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

    private Cell getAppropriateCell(Cell hero_cell, Cell suspicious, World world) {
        return world.getMap().getCell(hero_cell.getRow(), suspicious.getColumn());
    }

    private Cell getClosestCellsForDodge(World world, Hero hero, int dodge_board) {
        Cell[] cells = world.getMap().getObjectiveZone();
        Cell minCell = null;
        int O_O = 0;
        PriorityQueue<NiceCell> priorityQueue = new PriorityQueue<>();
        for (Cell cell : cells) {
            if (!hero.getCurrentCell().isInObjectiveZone()) {
                if (!isOccupied(cell, world) && !Blocked_Cells.contains(cell) && !blocks.contains(cell)
                        && world.manhattanDistance(hero.getCurrentCell(), cell) <= dodge_board && O_O <
                        world.manhattanDistance(hero.getCurrentCell(), cell)) {
                    minCell = cell;
                    O_O = world.manhattanDistance(hero.getCurrentCell(), cell);
                }
            } else {
                if (!isOccupied(cell, world) && !Blocked_Cells.contains(cell) && !blocks.contains(cell)
                        && world.manhattanDistance(hero.getCurrentCell(), cell) <= dodge_board && O_O <
                        world.manhattanDistance(hero.getCurrentCell(), cell)) {
                    NiceCell niceCell = new NiceCell();
                    if (hero.getName().equals(HeroName.HEALER)) {
                        niceCell.score = getCellScore(hero, world, cell,
                                null, hero, true, 1);
                    } else {
                        niceCell.score = getCellScore(hero, world, cell,
                                null, null, false, 1);
                    }
                    niceCell.direction = null;
                    niceCell.hero = hero;
                    niceCell.heroName = HeroName.BLASTER.toString();
                    niceCell.target = cell;
                    priorityQueue.add(niceCell);
                }
            }
        }
        if (priorityQueue.size() != 0) {
            minCell = priorityQueue.poll().target;
        }
        return minCell;
    }

    //finding the best cell to move
    private void finding_good_cell_to_move(Hero[] My_heroes, Hero hero, World world) {
        boolean isSentry = false, isBlaster = false, isHealer = false, isGuardian = false;
        Hero Healer = null;
        for (Hero hero1 : My_heroes) {
            if (hero1.getCurrentHP() >= 0) {
                if (hero1.getName().equals(HeroName.HEALER)) {
                    Healer = hero1;
                    break;
                }
            }
        }
        Hero intended_hero = null;
        double max;
        Direction direction;
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

        if (isSentry) {
            heroName = "sentry";
        } else if (isBlaster) {
            heroName = "blaster";
        } else if (isHealer) {
            heroName = "healer";
        } else if (isGuardian) {
            heroName = "guardian";
        }

        //dont move
        {
            score = getCellScore(hero, world, targetCell, null, Healer, isHealer);
            int y = hero.getMoveAPCost();
            score += (y) * 0.1;
            if (hero_cell_pair.containsKey(hero) &&
                    hero_turn_pair.get(hero) >= 2) {
//                score -= 1.5;
                hero_cell_pair.remove(hero);
                hero_turn_pair.remove(hero);
            }
            niceTargetRow = cell.getRow();
            niceTargetColl = cell.getColumn();
            direction = null;
            NiceCell nice_cell = new NiceCell();
            nice_cell.score = score;
            nice_cell.hero = world.getMyHero(cell);
            nice_cell.target = world.getMap().getCell(niceTargetRow, niceTargetColl);
            nice_cell.heroName = heroName;
            nice_cell.direction = direction;
            niceCells.add(nice_cell);
        }

        //moving up
        if (world.getMap().isInMap(cell.getRow() - 1, cell.getColumn()) &&
                !world.getMap().getCell(cell.getRow() - 1, cell.getColumn()).isWall()) {

            targetCell = world.getMap().getCell(cell.getRow() - 1, cell.getColumn());
            score = getCellScore(hero, world, targetCell, Direction.UP, Healer, isHealer);
            niceTargetRow = cell.getRow() - 1;
            niceTargetColl = cell.getColumn();
            intended_hero = world.getMyHero(cell);
            direction = Direction.UP;
            NiceCell nice_cell = new NiceCell();
            nice_cell.score = score;
            nice_cell.hero = world.getMyHero(cell);
            nice_cell.target = world.getMap().getCell(niceTargetRow, niceTargetColl);
            nice_cell.heroName = heroName;
            nice_cell.direction = direction;
            niceCells.add(nice_cell);

        }

        //moving down
        if (world.getMap().isInMap(cell.getRow() + 1, cell.getColumn()) &&
                !world.getMap().getCell(cell.getRow() + 1, cell.getColumn()).isWall()) {

            targetCell = world.getMap().getCell(cell.getRow() + 1, cell.getColumn());
            score = getCellScore(hero, world, targetCell, Direction.DOWN, Healer, isHealer);
            Cell goodCell = getClosestCells(world, hero).get(0);
            Direction direction1[] = world.getPathMoveDirections(hero.getCurrentCell(), goodCell, Blocked_Cells);
            System.out.println("Block size:" + Blocked_Cells.size());
            if (world.getCurrentTurn() == 4 && (hero.getName().equals(HeroName.SENTRY) || hero.getName().equals(HeroName.BLASTER))) {
                System.err.println("hero goes to good cell," + hero.getName() + ",good row:" + hero.getCurrentCell().getRow() + ",column:" + goodCell.getColumn()
                        + ",hero row:" + hero.getCurrentCell().getRow() + ",column:" + hero.getCurrentCell().getColumn());
                for (int i = 0; i < direction1.length; i++) {
                    System.out.println("direction i:" + i + "," + direction1[i]);
                }
            }
            niceTargetRow = cell.getRow() + 1;
            niceTargetColl = cell.getColumn();
            direction = Direction.DOWN;
            intended_hero = world.getMyHero(cell);
            NiceCell nice_cell = new NiceCell();
            nice_cell.score = score;
            nice_cell.hero = world.getMyHero(cell);
            nice_cell.target = world.getMap().getCell(niceTargetRow, niceTargetColl);
            nice_cell.heroName = heroName;
            nice_cell.direction = direction;
            niceCells.add(nice_cell);

        }

        //moving left
        if (world.getMap().isInMap(cell.getRow(), cell.getColumn() - 1) &&
                !world.getMap().getCell(cell.getRow(), cell.getColumn() - 1).isWall()) {

            targetCell = world.getMap().getCell(cell.getRow(), cell.getColumn() - 1);
            score = getCellScore(hero, world, targetCell, Direction.LEFT, Healer, isHealer);
            niceTargetRow = cell.getRow();
            niceTargetColl = cell.getColumn() - 1;
            direction = Direction.LEFT;
            intended_hero = world.getMyHero(cell);
            NiceCell nice_cell = new NiceCell();
            nice_cell.score = score;
            nice_cell.hero = world.getMyHero(cell);
            nice_cell.target = world.getMap().getCell(niceTargetRow, niceTargetColl);
            nice_cell.heroName = heroName;
            nice_cell.direction = direction;
            niceCells.add(nice_cell);
        }

        //moving right
        if (world.getMap().isInMap(cell.getRow(), cell.getColumn() + 1) &&
                !world.getMap().getCell(cell.getRow(), cell.getColumn() + 1).isWall()) {

            targetCell = world.getMap().getCell(cell.getRow(), cell.getColumn() + 1);
            score = getCellScore(hero, world, targetCell, Direction.RIGHT, Healer, isHealer);
            NiceCell nice_cell = new NiceCell();
            niceTargetRow = cell.getRow();
            niceTargetColl = cell.getColumn() + 1;
            direction = Direction.RIGHT;
            nice_cell.score = score;
            nice_cell.hero = world.getMyHero(cell);
            nice_cell.target = world.getMap().getCell(niceTargetRow, niceTargetColl);
            nice_cell.heroName = heroName;
            nice_cell.direction = direction;
            niceCells.add(nice_cell);
        }
        System.out.println("in the for:hero name:" + heroName + ",score:" + score);
        System.out.println("niceCells size is:" + niceCells.size());
    }

    //finding good cell for my blaster to throw a bomb
    private Cell find_good_op_for_blaster(World world, Hero myHero) {

        Cell goodCell = null;
        Cell myCell = myHero.getCurrentCell();
        Hero[] opHeroes = world.getOppHeroes();
        int maxScore = -1;
        int range = myHero.getAbility(AbilityName.BLASTER_BOMB).getRange();

        List<Cell> cells = new ArrayList<>();

        for (int row = myCell.getRow() - range; row <= myCell.getRow() + range; row++) {
            for (int col = myCell.getColumn() - range; col <= myCell.getColumn() + range; col++) {
                Cell cell = world.getMap().getCell(row, col);
                if (world.manhattanDistance(cell, myCell) <= myHero.getAbility(AbilityName.BLASTER_BOMB).getRange())
                    cells.add(cell);
            }
        }


        for (int i = 0; i < cells.size(); i++) {
            int score = 0;

            for (Hero op : opHeroes) {

                Cell cell = cells.get(i);
                Cell opCell = op.getCurrentCell();

                if (!opCell.isInVision())
                    continue;

                if (world.manhattanDistance(cell, opCell) <= myHero.getAbility(AbilityName.BLASTER_BOMB).getAreaOfEffect())
                    score++;
            }

            if (maxScore < score) {
                maxScore = score;
                goodCell = myCell;
            }

        }

        if (maxScore == 1) {
            int minHp = 1000;
            for (Hero op : opHeroes) {

                if (!op.getCurrentCell().isInVision())
                    continue;

                if (op.getCurrentHP() < minHp) {
                    if (world.manhattanDistance(myCell, op.getCurrentCell()) <= myHero.getAbility(AbilityName.BLASTER_BOMB).getRange() +
                            myHero.getAbility(AbilityName.BLASTER_BOMB).getAreaOfEffect()) {
                        minHp = op.getCurrentHP();
                        goodCell = op.getCurrentCell();
                    }
                }
            }
        }

        return goodCell;
    }

    private double getCellScore(Hero hero, World world, Cell targetCell, Direction direction, Hero Healer,
                                Boolean isHealer) {
        if (hero.getCurrentHP() <= 0) {
            return -999;
        }
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
            if (name.equals(HeroName.GUARDIAN)) {
                int minHP = 1000;
                Hero tHero = null;
                if (attackReady) {
                    Boolean motamarkez = false;
                    for (Hero dHero : oppHeroes) {
                        if (dHero.getCurrentHP() == 0)
                            continue;
                        int guardians = 0;
                        int O_o = 0;
                        for (; O_o < world.getOppHeroes().length; O_o++) {
                            if (world.getOppHeroes()[O_o].getName().equals(HeroName.GUARDIAN)) {
                                guardians++;
                            }
                        }
                        if (dHero.getName().equals(HeroName.GUARDIAN) && (guardians == 1 || (dHero.getCurrentHP()
                                > hero.getCurrentHP() && guardians != 4))) {
                            continue;
                        }
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
                        if (tHero != null && world.manhattanDistance(targetCell, tHero.getCurrentCell()) <= 1) {
                            if (direction == null) {
                                score += 10;
                            }
                        }
                        if (tHero != null && world.getPathMoveDirections(cell, tHero.getCurrentCell(), Blocked_Cells).length > 0) {
                            if (direction == world.getPathMoveDirections(cell, tHero.getCurrentCell(), Blocked_Cells)[0]) {
                                if (faseleFromObjective(world, tHero.getCurrentCell()) < 4 ||
                                        world.manhattanDistance(cell, tHero.getCurrentCell()) < 2) {
                                    score += 3;
                                }
                            }
                        }

                    } else {
                        minHP = 1000;
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
                                if (direction == world.getPathMoveDirections(cell, tHero.getCurrentCell(), Blocked_Cells)[0]) {
                                    if (tHero.getCurrentCell().isInObjectiveZone() ||
                                            world.manhattanDistance(cell, tHero.getCurrentCell()) < 2) {
                                        score += 3;
                                    }
                                }
                            }
                        }
                    }

                } else {
                    Boolean b = true;
                    int asli = -1000;
                    Cell thatCell = null;
                    for (int i = -3; i <= 3; i++) {
                        for (int j = Math.abs(i) - 3; j <= 3 - Math.abs(i); j++) {
                            int sco = 1000;
                            Cell bCell = world.getMap().getCell(hero.getCurrentCell().getRow() + i
                                    , hero.getCurrentCell().getColumn() + j);
                            for (Hero uHero : oppHeroes) {

                                if (!uHero.getCurrentCell().isInVision() || uHero.getCurrentHP() <= 0)
                                    continue;
                                for (Ability ab : uHero.getAbilities()) {
                                    if (!isOpppowerReady(world, uHero.getId()) && !isOppattackReady(world, uHero.getId())) {
                                        break;
                                    }
                                    AbilityName aname = ab.getName();
                                    if (aname == AbilityName.GUARDIAN_DODGE || aname == AbilityName.HEALER_DODGE
                                            || aname == AbilityName.BLASTER_DODGE || aname == AbilityName.SENTRY_DODGE)
                                        continue;

                                    if (world.manhattanDistance(bCell, uHero.getCurrentCell()) <=
                                            ab.getRange() + ab.getAreaOfEffect())
                                        sco -= 1;
                                }

                            }
                            if (sco > asli) {
                                asli = sco;
                                thatCell = bCell;
                            }
                        }

                    }
                    if ((direction == null && world.getPathMoveDirections(cell, thatCell).length == 0)
                            || direction == world.getPathMoveDirections(cell, thatCell)[0])
                        score += 20;
                }
            }

            if (name.equals(HeroName.BLASTER)) {
                if (faseleFromObjective(world, hero.getCurrentCell()) < 3 && (attackReady || powerReady)) {
                    if (direction == null && hero.getCurrentCell().equals(targetCell)) {
                        score += 1;
                    }
                }
                if ((!powerReady) && (!attackReady)) {
                    //now should scape
                    int asli = -1000;
                    Cell thatCell = null;
                    for (int i = -3; i <= 3; i++) {
                        for (int j = Math.abs(i) - 3; j <= 3 - Math.abs(i); j++) {
                            int sco = 1000;
                            Cell bCell = world.getMap().getCell(hero.getCurrentCell().getRow() + i
                                    , hero.getCurrentCell().getColumn() + j);
                            for (Hero uHero : oppHeroes) {

                                if (!uHero.getCurrentCell().isInVision() || uHero.getCurrentHP() <= 0)
                                    continue;
                                for (Ability ab : uHero.getAbilities()) {
                                    if (!isOpppowerReady(world, uHero.getId()) && !isOppattackReady(world, uHero.getId())) {
                                        break;
                                    }
                                    AbilityName aname = ab.getName();
                                    if (aname == AbilityName.GUARDIAN_DODGE || aname == AbilityName.HEALER_DODGE
                                            || aname == AbilityName.BLASTER_DODGE || aname == AbilityName.SENTRY_DODGE)
                                        continue;

                                    if (world.manhattanDistance(bCell, uHero.getCurrentCell()) <=
                                            ab.getRange() + ab.getAreaOfEffect() + 1)
                                        sco -= 1;
                                }

                            }
                            if (sco > asli) {
                                asli = sco;
                                thatCell = bCell;
                            }
                        }

                    }
                    if ((direction == null && world.getPathMoveDirections(cell, thatCell).length == 0)
                            || direction == world.getPathMoveDirections(cell, thatCell)[0])
                        score += 20;
                }
                for (Hero oHero : oppHeroes) {
                    if (!oHero.getCurrentCell().isInVision() || oHero.getCurrentHP() <= 0)
                        continue;
                    Cell heroCell = oHero.getCurrentCell();
//                    if (oHero.getName().equals(HeroName.GUARDIAN)) {
//                        if (powerReady && world.manhattanDistance(heroCell, targetCell)
//                                >= powerRange + powerAbility.getAreaOfEffect() &&
//                                world.manhattanDistance(heroCell, targetCell) < 8) {
//                            score -= (world.manhattanDistance(heroCell, targetCell) - 7) / 2;
//                        }
//                        if (world.manhattanDistance(heroCell, targetCell) < 8) {
//                            score -= (7 - world.manhattanDistance(heroCell, targetCell)) / 2;
//                        }
//
//                        if (world.isInVision(targetCell, heroCell) && attackReady
//                                && world.manhattanDistance(heroCell, targetCell)
//                                >= attackRange + attackAbility.getAreaOfEffect() && world.manhattanDistance(heroCell,
//                                targetCell) < 6) {
//                            score -= (world.manhattanDistance(heroCell, targetCell) - 4) / 2;
//                        }
//                        if (world.manhattanDistance(heroCell, targetCell) < 5) {
//                            score -= (4 - world.manhattanDistance(heroCell, targetCell)) / 2;
//                        }
//                    }
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
        if (name.equals(HeroName.SHADOW)) {
            if (!hero.getAbility(AbilityName.SHADOW_SLASH).isReady()) {
                for (int i = 0; i < world.getOppHeroes().length; i++) {
                    if (world.manhattanDistance(world.getOppHeroes()[i].getCurrentCell(), targetCell) < 7) {
                        score -= 7 - world.manhattanDistance(world.getOppHeroes()[i].getCurrentCell(), targetCell);
                    }
                }
            } else {
                for (int i = 0; i < world.getOppHeroes().length; i++) {
                    if (world.manhattanDistance(world.getOppHeroes()[i].getCurrentCell(), targetCell) < 21
                            && world.manhattanDistance(world.getOppHeroes()[i].getCurrentCell(), targetCell) > 6)
                        score += 20 - world.manhattanDistance(world.getOppHeroes()[i].getCurrentCell(), targetCell);
                }
            }

        }
        if (name.equals(HeroName.SENTRY)) {
            if (targetCell.isInObjectiveZone())
                score += 0.5;

            Cell target = find_good_cell_for_sentry_to_go(world, hero);
            if (hero.getAbility(AbilityName.SENTRY_RAY).isReady() && target != null) {
                System.out.println("good Cell for sentry to go there for its ray" +
                        "(" + target.getRow() + "," + target.getColumn() + ")");
                if (world.getPathMoveDirections(cell, target, Blocked_Cells).length != 0 &&
                        world.getPathMoveDirections(cell, target, Blocked_Cells)[0] == direction)
                    score += 5;
            }

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
                        if (direction == null && hero.getCurrentCell().equals(targetCell)) {
                            score += 0.75;
                        }
                        break;
                    }
                    else if (world.manhattanDistance(heroCell, targetCell)
                            <= hero.getAbility(AbilityName.SENTRY_ATTACK).getRange()) {
                        score += 1.5;
                        if (direction == null) {
                            score += 0.75;
                        }
                        break;
                    }
                }
            }
            for (Hero oHero : oppHeroes) {
                Cell heroCell = oHero.getCurrentCell();
                if (!heroCell.isInVision()) {
                    continue;
                }
                if (!isOpppowerReady(world, oHero.getId()) && !isOppattackReady(world, oHero.getId())) {
                    continue;
                }
                if (world.manhattanDistance(heroCell, hero.getCurrentCell()) <= 5 &&
                        oHero.getName().equals(HeroName.BLASTER) ) {
                    score -= (5 - world.manhattanDistance(heroCell, targetCell));
                }
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

        if ((name.equals(HeroName.BLASTER) && hero.getAbility(AbilityName.BLASTER_BOMB).isReady()) ||
                name.equals(HeroName.HEALER)) {
            Cell[] objective = world.getMap().getObjectiveZone();
            Boolean notVis = true;
            for (Hero oppHero : oppHeroes) {
                if (world.isInVision(oppHero.getCurrentCell(), targetCell)) {
                    notVis = false;
                    break;
                }
            }
            if (notVis && faseleFromObjective(world,hero.getCurrentCell()) <= 2) {
                score += 2;
            }
        } else if (name.equals(HeroName.HEALER)) {
            for (Hero oHero : oppHeroes) {
                Cell heroCell = oHero.getCurrentCell();
                if (world.manhattanDistance(heroCell, hero.getCurrentCell()) <= 7 &&
                        oHero.getName().equals(HeroName.BLASTER) && oHero.getCurrentHP() > 100) {
                    score -= (7 - world.manhattanDistance(heroCell, targetCell)) / 2;
                }
            }
        }


        if (Healer != null) {
            Cell healerCell = Healer.getCurrentCell();
            if (!isHealer && world.manhattanDistance(cell, healerCell) <= 4)
                score += .5;
        }

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
            if (oppHero.getName().equals(HeroName.GUARDIAN) && !name.equals(HeroName.GUARDIAN)) {

                if (world.manhattanDistance(targetCell, oppCell) < 3) {
                    score -= (3 - world.manhattanDistance(targetCell, oppCell)) * 2;
                    System.out.println("Near of Guardian");
                }
            }
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

            if (oppHero.getName().equals(HeroName.SHADOW)) {
                for (int i = 0; i < world.getMyHeroes().length; i++) {
                    if (world.getMyHeroes()[i].getCurrentHP() == 0) {
                        continue;
                    }
                    if (world.isInVision(world.getMyHeroes()[i].getCurrentCell(), oppCell)) {
                        for (int j = 0; j < world.getMyHeroes().length; j++) {
                            if (world.getMyHeroes()[j].equals(world.getMyHeroes()[i])
                                    || world.getMyHeroes()[j].getCurrentHP() <= 0) {
                                continue;
                            }
                            Cell myHeroCell = world.getMyHeroes()[i].getCurrentCell();
                            if (1 == squareCollision(targetCell
                                    , world.getMyHeroes()[j].getCurrentCell(), oppHero.getCurrentCell()) &&
                                    world.manhattanDistance(world.getMyHeroes()[j].getCurrentCell()
                                            , oppHero.getCurrentCell()) < 7) {
                                ArrayList<Cell> arrayList = new ArrayList<>();
                                if (world.getMap().
                                        getCell(myHeroCell.getRow(), myHeroCell.getColumn() - 1).isInVision()) {
                                    arrayList.add(world.getMap().
                                            getCell(myHeroCell.getRow(), myHeroCell.getColumn() - 1));
                                } else if (world.getMap().
                                        getCell(myHeroCell.getRow(), myHeroCell.getColumn() + 1).isInVision()) {
                                    arrayList.add(world.getMap().
                                            getCell(myHeroCell.getRow(), myHeroCell.getColumn() + 1));
                                }
                                if (world.getMap().
                                        getCell(myHeroCell.getRow() + 1, myHeroCell.getColumn()).isInVision()) {
                                    arrayList.add(world.getMap().
                                            getCell(myHeroCell.getRow() + 1, myHeroCell.getColumn()));
                                }
                                if (world.getMap().
                                        getCell(myHeroCell.getRow() - 1, myHeroCell.getColumn()).isInVision()) {
                                    arrayList.add(world.getMap().
                                            getCell(myHeroCell.getRow() - 1, myHeroCell.getColumn()));
                                }

                                for (int k = 0; k < arrayList.size(); k++) {
                                    if (1 != squareCollision(targetCell
                                            , world.getMyHeroes()[j].getCurrentCell(), oppHero.getCurrentCell())) {
                                        if (world.getPathMoveDirections(targetCell, arrayList.get(k)).length != 0) {
                                        }
                                    }
                                }
                                score -= 3;
                            }
                        }
                    }
                }
            }

        }
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
                if (cells[i][j].isWall() && (world.manhattanDistance(cells[i][j], getClosestZoneCells(world, cells[i][j])) < 4))
                    dis.add(world.manhattanDistance(i, j, 0, 0));
            }
        }
        for (int i = 0; i < 4; i++) {
            System.out.println("col:" + world.getMap().getMyRespawnZone()[i].getColumn()
                    + "," + world.getMap().getMyRespawnZone()[i].getRow());
        }
        System.out.println("sentry to be or not to be? " + sentry_ToBe_orNotToBe(world));
        if (sentry_ToBe_orNotToBe(world) > 0.9) {
            extraSentry = true;
        }

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
            world.pickHero(HeroName.BLASTER);
            pick_period++;
        } else {
//            if (extraSentry) {
//                world.pickHero(HeroName.SENTRY);
//            } else {
                world.pickHero(HeroName.BLASTER);
//            }
            pick_period = 0;
        }
    }

    public void moveTurn(World world) {
        System.out.println("move started");
        System.out.println("current ap:" + world.getAP() + ",in turn:" + world.getCurrentTurn() + ",phase:" + world.getMovePhaseNum());
        for (int i = 0; i < world.getMyCastAbilities().length; i++) {
            System.out.println(world.getMyCastAbilities()[i].getAbilityName() + ",id:"
                    + world.getMyCastAbilities()[i].getCasterId());
        }
        int O_o = (int) System.currentTimeMillis();
        Hero My_hero;
        if (pick_period == 0) {
            for (int i = 0; i < world.getMyHeroes().length; i++) {
                System.out.println("id:" + world.getMyHeroes()[i].getId());
            }
            for (int i = 0; i < world.getOppHeroes().length; i++) {
                System.out.println("Opp id:" + world.getOppHeroes()[i].getId());
            }
            pick_period = 1;
        }
        Hero[] heroes = world.getMyHeroes();
        System.out.println("Phase in move:" + world.getMovePhaseNum());
        Blocked_Cells.clear();
        for (int j = 0; j < world.getMyHeroes().length; j++) {
            Blocked_Cells.add(world.getMyHeroes()[j].getCurrentCell());
        }


        oppCastAbilities.clear();
        if (world.getMovePhaseNum() == 0) {
            for (CastAbility ability : world.getOppCastAbilities()) {
                System.out.println("Opp ability :" + ability.getAbilityName() + ", bye " + ability.getCasterId());
                oppCastAbilities.add(ability);
                OppDetails oppDetails = new OppDetails(ability, world.getCurrentTurn() - 1);
                oppDetailsList.add(oppDetails);

                for (int i = 0; i < world.getOppHeroes().length; i++) {
                    System.out.println("Opp id:" + world.getOppHeroes()[i].getId() + "is PowerReady:" +
                            isOpppowerReady(world, world.getOppHeroes()[i].getId()) + ",is AttackReady: " +
                            isOppattackReady(world, world.getOppHeroes()[i].getId()));
                }
            }
        }


        for (int i = 0; i < 4; i++) {
            My_hero = world.getMyHeroes()[i];
            if (My_hero.getCurrentHP() <= 0) {
                continue;
            }
            System.out.println("turn: " + world.getCurrentTurn());
            finding_good_cell_to_move(heroes, My_hero, world);
            NiceCell niceCell;
            System.out.println("nicecells size:" + niceCells.size());
            niceCell = niceCells.poll();
            if (niceCells.size() != 0 && isPeriodic(My_hero, world, i, world.getMovePhaseNum())) {
                niceCell = niceCells.poll();
            }
            System.out.println("blocked size: " + Blocked_Cells.size());
            System.out.println("direction for hero to move in nice cell: " + niceCell.direction);
            System.out.println("hero in nice cell: " + niceCell.hero
                    + ",and its score:" + niceCell.score);
            if (niceCell.direction != null) {
                heroCellArray[world.getMovePhaseNum()][i] = niceCell.target;
                world.moveHero(niceCell.hero, niceCell.direction);
                System.out.println("current row:" + My_hero.getCurrentCell().getRow() + ",col:" +
                        My_hero.getCurrentCell().getColumn() + ",dir:" + niceCell.direction);
                System.out.println("row:" + niceCell.target.getRow() + ",column:" + niceCell.target.getColumn());
                if (!Arrays.asList(world.getMyDeadHeroes()).contains(My_hero)
                        && world.getAP() >= niceCell.hero.getMoveAPCost()) {
                    Blocked_Cells.remove(niceCell.hero.getCurrentCell());
                    Blocked_Cells.add(niceCell.target);
                }
            }
            niceCells.clear();
            myHeroesHp.put(My_hero, My_hero.getCurrentHP());
        }
        Blocked_Cells.clear();
        System.out.println("elapsed time for move Turn is:" + ((int) System.currentTimeMillis() - O_o) + "," + System.currentTimeMillis());
    }

    private Cell find_good_cell_for_sentry_to_go(World world, Hero hero) {

        Cell goodCell = null;
        Cell[][] cells = world.getMap().getCells();
        Cell[] objectiveZone = world.getMap().getObjectiveZone();
        Cell[] myRespawn = world.getMap().getMyRespawnZone();
        Cell[] oppRespawn = world.getMap().getOppRespawnZone();
        double maxScore = -1;
        int sum_col = 0, sum_row = 0;
        int ave_col = 0, ave_row = 0;
        for (Cell objective : objectiveZone) {
            sum_col += objective.getColumn();
            sum_row += objective.getRow();
        }
        ave_col = Math.round(sum_col / objectiveZone.length);
        ave_row = Math.round(sum_row / objectiveZone.length);
        Cell ave = world.getMap().getCell(ave_row, ave_col);
        for (Cell[] cell : cells) {
            for (Cell c : cell) {
                int score = 0;
                for (Cell objective : objectiveZone) {
                    if (hero.getAbility(AbilityName.SENTRY_RAY).isReady()) {
                        if (world.isInVision(c, objective) && world.manhattanDistance(c, ave) >= 8
                                && world.manhattanDistance(hero.getCurrentCell(), ave) >= 7)
                            score++;
                    }
                }

                if (world.manhattanDistance(c, myRespawn[0]) > world.manhattanDistance(c, oppRespawn[0])) {
                    continue;
                }
                if (maxScore < score) {
                    maxScore = score;
                    goodCell = c;
                }
            }
        }
        System.out.println("max score in sentry choosing:" + maxScore);
        return goodCell;
    }

    private double sentry_ToBe_orNotToBe(World world) {

        Cell[][] cells = world.getMap().getCells();
        Cell[] objectiveZone = world.getMap().getObjectiveZone();
        Cell[] myRespawn = world.getMap().getMyRespawnZone();
        Cell[] oppRespawn = world.getMap().getOppRespawnZone();
        double maxScore = -1;
        Cell ave;
        int sum_col = 0, sum_row = 0;
        int ave_col = 0, ave_row = 0;
        for (Cell objective : objectiveZone) {
            sum_col += objective.getColumn();
            sum_row += objective.getRow();
        }
        ave_col = Math.round(sum_col / objectiveZone.length);
        ave_row = Math.round(sum_row / objectiveZone.length);
        ave = world.getMap().getCell(ave_row, ave_col);
        System.out.println("ave col :" + ave_col + ",ave_row:" + ave_row);
        for (Cell[] cell : cells) {
            for (Cell c : cell) {
                int score = 0;
                for (Cell objective : objectiveZone) {
                    if (world.isInVision(c, objective) && world.manhattanDistance(c, ave) >= 10)
                        score++;
                }

                if (world.manhattanDistance(c, myRespawn[0]) > world.manhattanDistance(c, oppRespawn[0])) {
                    continue;
                }
                if (maxScore < score) {
                    maxScore = score;
                }
            }
        }
        System.out.println("max score in sentry choosing:" + maxScore + " lenght is:"
                + objectiveZone.length + "and percent is :" +
                maxScore / objectiveZone.length);
        return maxScore / objectiveZone.length;
    }

    private boolean isPeriodic(Hero MyHero, World world, int i, int phaseNum) {
        if (phaseNum < 2) {
            return false;
        }
        int fre1 = 0, fre2 = 0;
        for (int j = 2; j <= phaseNum; j++) {
            if (j % 2 == 0) {
                if (heroCellArray[j - 2][i] != null && heroCellArray[j][i] != null && heroCellArray[j][i]
                        .equals(heroCellArray[j - 2][i])) {
                    fre1++;
                }
            } else {
                if (heroCellArray[j - 2][i] != null && heroCellArray[j][i] != null && heroCellArray[j][i]
                        .equals(heroCellArray[j - 2][i])) {
                    fre2++;

                }
            }
        }

        return fre1 >= 2 || fre2 >= 2;
    }

    public void actionTurn(World world) {

        System.out.println("action " + world.getCurrentTurn() + ",started" + ",current ap:" + world.getAP());
        Hero[] heroes = world.getMyHeroes();
        Hero[] Opp_Heroes = world.getOppHeroes();
        ArrayList<Hero> target_heroes = new ArrayList<>();
        int o_o = (int) System.currentTimeMillis();
        System.out.println("The start of action:" + o_o);
        int O = world.getMyDeadHeroes().length;
        boolean for_guardian = false;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                heroCellArray[i][j] = null;
            }
        }
        for (int i = 0; i < world.getMyCastAbilities().length; i++) {
            System.out.println(world.getMyCastAbilities()[i]);
        }
        for (Hero hero : heroes) {
            O++;
            boolean flag = true;
            for (int i = 0; i < world.getMyDeadHeroes().length; i++) {
                System.out.println("O_o:" + world.getMyDeadHeroes()[i].getCurrentHP());
            }
            if (hero.getCurrentHP() <= 0) {
                continue;
            }
            boolean attackReady = false;
            if (hero.getName().equals(HeroName.GUARDIAN)) {
                if (hero.getAbility(AbilityName.GUARDIAN_ATTACK).isReady()) {
                    attackReady = true;
                }
            }
            Cell hero_cell = hero.getCurrentCell();
            for (int i = 0; i < world.getOppHeroes().length; i++) {
                if (world.getOppHeroes()[i].getName().equals(HeroName.GUARDIAN) && !hero.getName().equals(HeroName.GUARDIAN)
                        && (world.getOppHeroes()[i].getCurrentHP() > 50)
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
                        Cell targetCell = find_good_op_for_blaster(world, hero);
                        world.castAbility(hero, AbilityName.BLASTER_BOMB, targetCell);
                        System.out.println("blaster id:" + hero.getId() + " bombed and end:" +
                                "col:" + targetCell.getColumn() + ",row:" + targetCell.getRow());
                    }
                    if (hero.getAbility(AbilityName.BLASTER_ATTACK).isReady()) {
                        System.out.println("Blaster attack is ready and turn:" + world.getCurrentTurn());
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
                            System.out.println("blaster id:" + hero.getId() + " attacked and end:" + goodOpp.getCurrentCell());
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
                        System.out.println("Guardian attack is ready and turn:" + world.getCurrentTurn());
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
                            for_guardian = true;
                            if (Math.abs(hero_cell.getColumn() - goodOpp.getCurrentCell().getColumn()) == 1 &&
                                    Math.abs(hero_cell.getRow() - goodOpp.getCurrentCell().getRow()) == 1) {
                                world.castAbility(hero, AbilityName.GUARDIAN_ATTACK, getAppropriateCell(hero_cell, goodOpp.getCurrentCell(), world));
                            } else {
                                world.castAbility(hero, AbilityName.GUARDIAN_ATTACK, goodOpp.getCurrentCell());
                            }
                            System.out.println("guardian attacked and end:" + goodOpp.getCurrentCell().getRow() + ",col:"
                                    + goodOpp.getCurrentCell().getColumn());
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
                    guardianFortify(hero, world);
                }else if (hero.getName().equals(HeroName.SHADOW)){
                    int minHP = 1000;
                    Hero goodOpp = null;
                    if (hero.getAbility(AbilityName.SHADOW_SLASH).isReady()) {
                        for (Hero oppHero : Opp_Heroes) {
                            if (!oppHero.getCurrentCell().isInVision())
                                continue;
                            if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell())
                                    - hero.getAbility(AbilityName.SHADOW_SLASH).getAreaOfEffect() <=
                                    hero.getAbility(AbilityName.SHADOW_SLASH).getRange()
                                    && world.isInVision(hero_cell, oppHero.getCurrentCell())) {

                                if (oppHero.getCurrentHP() < minHP) {
                                    minHP = oppHero.getCurrentHP();
                                    goodOpp = oppHero;
                                }
                            }
                        }
                        if (goodOpp != null) {
                            world.castAbility(hero, AbilityName.SHADOW_SLASH, goodOpp.getCurrentCell());
                        }
                    }else if (hero.getAbility(AbilityName.SHADOW_DODGE).isReady()){
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
                                    niceCell.heroName = HeroName.SHADOW.toString();
                                    niceCell.target = world.getMap().getCell(i, j);
                                    priorityQueue.add(niceCell);
                                } else if (!isOccupied(world.getMap().getCell(i, j), world) &&
                                        world.manhattanDistance(world.getMap().getCell(i, j), hero_cell) == 3) {
                                    NiceCell niceCell = new NiceCell();
                                    niceCell.score = getCellScore(hero, world, world.getMap().getCell(i, j),
                                            null, null, false);
                                    niceCell.direction = null;
                                    niceCell.hero = hero;
                                    niceCell.heroName = HeroName.SHADOW.toString();
                                    niceCell.target = world.getMap().getCell(i, j);
                                    priorityQueue.add(niceCell);
                                }
                            }
                        }
                        System.out.println("Shadow dodged");
                        world.castAbility(hero, AbilityName.SHADOW_DODGE, priorityQueue.poll().target);
                    }else if (hero.getAbility(AbilityName.SHADOW_ATTACK).isReady()){
                        goodOpp = null;
                        for (Hero oppHero : Opp_Heroes) {
                            if (!oppHero.getCurrentCell().isInVision())
                                continue;
                            if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell()) <=
                                    hero.getAbility(AbilityName.SHADOW_ATTACK).getRange()) {

                                if (oppHero.getCurrentHP() < minHP) {
                                    minHP = oppHero.getCurrentHP();
                                    goodOpp = oppHero;
                                }
                            }
                        }
                        if (goodOpp != null) {
                            world.castAbility(hero, AbilityName.SHADOW_ATTACK, goodOpp.getCurrentCell());
                            System.out.println("shadow attacked and end:" + goodOpp.getCurrentCell());
                        }
                    }
                }else {
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
                            for (int i = 0; i < world.getMyHeroes().length; i++) {
                                if (world.getMyHeroes()[i].getCurrentHP() >= 0 && world.getMyHeroes()[i].getName().equals(HeroName.GUARDIAN)) {
                                    if (world.getMyHeroes()[i].getCurrentHP() < 150) {
                                        injury = world.getMyHeroes()[i];
                                        break;
                                    }
                                }
                            }
                            world.castAbility(hero, AbilityName.HEALER_HEAL, injury.getCurrentCell());
                            System.out.println("healer healed , end:" + injury.getCurrentCell());
                        }
                    }
                }
            }
            boolean flag2 = false;
            if (for_guardian || O == 4) {
                for (int i = 0; i < world.getOppHeroes().length; i++) {
                    if (world.manhattanDistance(world.getOppHeroes()[i].getCurrentCell(), hero_cell) < 5) {
                        flag2 = true;
                    }
                    if (world.getOppHeroes()[i].getName().equals(HeroName.BLASTER)
                            && world.manhattanDistance(world.getOppHeroes()[i].getCurrentCell(), hero_cell) < 8) {
                        flag2 = true;
                    }
                }
            }
            if (hero.getName().equals(HeroName.GUARDIAN) && flag && hero.getCurrentHP() >= 50) {
                continue;
            }
            //dodge
            System.out.println("flag:" + flag + ",flag2:" + flag2 + ",for_guardian:" + for_guardian);
            if (!flag || (!hero.getCurrentCell().isInObjectiveZone())) {
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
                        world.castAbility(hero, AbilityName.HEALER_DODGE, des);
                    }

                }
            }
        }

        Hero I_m_Guardian = null;
        for (int i = 0; i < world.getMyHeroes().length; i++) {
            for (int j = 0; j < world.getOppHeroes().length; j++) {
                Hero myHero = world.getMyHeroes()[i], Opphero = world.getOppHeroes()[j];
                if (myHero.getCurrentHP() <= 0 || !Opphero.getCurrentCell().isInVision()) {
                    continue;
                }
                if (world.manhattanDistance(myHero.getCurrentCell(), Opphero.getCurrentCell()) <= 7 &&
                        !Opphero.getName().equals(HeroName.GUARDIAN)) {
                    if (myHero.getName().equals(HeroName.GUARDIAN)) {
                        I_m_Guardian = myHero;
                    }
                    if (myHero.getCurrentHP() < 150) {
                        world.castAbility(myHero, AbilityName.GUARDIAN_DODGE, getClosestCellsForDodge(world, myHero, 2));
                    }
                    world.castAbility(myHero, AbilityName.BLASTER_DODGE, getClosestCellsForDodge(world, myHero, 4));

                }
            }
        }

        if (I_m_Guardian != null) {
            guardianFortify(I_m_Guardian, world);
            world.castAbility(I_m_Guardian, AbilityName.GUARDIAN_DODGE, getClosestCellsForDodge(world, I_m_Guardian, 2));
        }
        System.out.println("My score:" + world.getMyScore());
        System.out.println("Opp score:" + world.getOppScore());
        o_o = (int) (System.currentTimeMillis() - o_o);
        System.out.println("elapsed time for action Turn:" + o_o + "," + System.currentTimeMillis());
    }

    private void guardianFortify(Hero hero, World world) {
        Cell hero_cell = hero.getCurrentCell();
        if (hero.getAbility(AbilityName.GUARDIAN_FORTIFY).isReady()) {
            Cell des = null;
            for (Hero hero1 : world.getMyHeroes()) {
                if (hero1.getCurrentHP() <= 0 || !hero1.getCurrentCell().isInObjectiveZone()) {
                    continue;
                }
                if (hero1.getCurrentHP() <= hero1.getMaxHP() / 2 && world.manhattanDistance(hero_cell, des) <=
                        hero.getAbility(AbilityName.GUARDIAN_FORTIFY).getRange()) {
                    des = hero1.getCurrentCell();
                }
            }
            if (hero.getCurrentHP() < 200 && hero.getCurrentCell().isInObjectiveZone()) {
                des = hero_cell;
            }
            if (des != null) {
                world.castAbility(hero, AbilityName.GUARDIAN_FORTIFY, des);
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
