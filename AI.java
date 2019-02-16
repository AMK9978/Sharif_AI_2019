package client;

import client.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

public class AI {
    private Random random = new Random();
    private Map map;
    private int pick_period = 0;
    Boolean b = false;

    ArrayList<Cell> Blocked_Cells = new ArrayList<>();

    public void preProcess(World world) {
        System.out.println("pre process started");
        map = world.getMap();
        Cell[][] cells = map.getCells();
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < 31; j++) {
                if (cells[i][j].isWall()) {
                    Blocked_Cells.add(cells[i][j]);
                }
            }
        }
//        for (int i = 0; i < cells.length; i++) {
//            for (int j = 0; j < cells[i].length; j++) {
//                System.out.println("cell i:" + i + ",j:" + j);
//                System.out.println("isInRespawnZone?:" + cells[i][j].isInMyRespawnZone());
//                System.out.println("isInObjectiveZone?:" + cells[i][j].isInObjectiveZone());
//                System.out.println("isInVision?" + cells[i][j].isInVision());
//                System.out.println("isWall?" + cells[i][j].isWall());
//                System.out.println();
//            }
//
//            System.out.println();
//        }
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
            world.pickHero(HeroName.SENTRY);
            pick_period++;
        } else {
                if (random.nextInt() % 2 == 0) {
                    world.pickHero(HeroName.HEALER);
                } else {
                    world.pickHero(HeroName.GUARDIAN);
                }

            pick_period = 0;
        }
    }

//    public void checkPoss(World world,){
//        for (; j < 4; j++) {
//            if (world.getMap().getCell(hero.getCurrentCell().getRow() + 1,
//                    hero.getCurrentCell().getColumn()) == world.getMyHeroes()[i].getCurrentCell()){
//                continue;
//            }else{
//                return true;
//            }
//        }
//    }

    public void moveTurn(World world) {
        System.out.println("move started");

        for (int j = 0; j < world.getMyHeroes().length; j++) {
            Blocked_Cells.add(world.getMyHeroes()[j].getCurrentCell());
        }
        Cell[][] cells = world.getMap().getCells();
        Hero My_hero = null;
        System.out.println("Phase in move:" + world.getMovePhaseNum());
        Cell[] targets = world.getMap().getObjectiveZone();
        Cell target = targets[random.nextInt(targets.length)];
        for (int i = 0; i < 4; i++) {
            My_hero = world.getMyHeroes()[i];
            if (My_hero.getCurrentCell().isInObjectiveZone()) {
                continue;
            }
            My_hero = world.getMyHeroes()[i];
            Cell origin = My_hero.getCurrentCell();
            Direction directions[] =
                    world.getPathMoveDirections(origin, target, Blocked_Cells);
            if (directions.length == 0) {
                continue;
            }
            if (world.getCurrentTurn() > 30 && world.getAP() < 75) {
                i = 2;
            } else if (world.getAP() < 75) {
                if (i == 2) {
                    break;
                }
            }
            System.out.println("Length:" + directions.length);
            System.out.println("turn:" + world.getCurrentTurn() + ",id:" + My_hero.getId() + ",row:" + origin.getRow() + ",col:" + origin.getColumn());
            System.out.println("target row:" + target.getRow() + ",col:" + target.getColumn());

            System.out.println(world.getPathMoveDirections(origin, target, Blocked_Cells)[0].toString());
            Direction direction = directions[0];
            world.moveHero(My_hero, direction);
//            Cell destination;
//            if (direction.equals(Direction.DOWN)) {
//                destination = world.getMap().getCell(origin.getRow()+1, origin.getColumn());
//            } else if (direction.equals(Direction.UP)) {
//                destination = world.getMap().getCell(origin.getRow() -1,origin.getColumn());
//            } else if (direction.equals(Direction.LEFT)) {
//                destination = world.getMap().getCell(origin.getRow(), origin.getColumn() -1);
//            } else {
//                destination = world.getMap().getCell(origin.getRow(), origin.getColumn()+1);
//            }
//            System.out.println("Hero Column:" + My_hero.getCurrentCell().getColumn() + ",row:" + My_hero.getCurrentCell().getRow());
//            System.out.println("is the des wall?" + destination.isWall());
//            System.out.println("direction:" + direction.toString());
//            System.out.println("target:row"+target[0].getRow()+",column:"+target[0].getColumn());
//            for (int j = 0; j < directions.length; j++) {
//                System.out.println(j+":"+directions[j].toString());
//            }
//            System.out.println();
        }
    }
//        if (world.getMovePhaseNum() == 0) {
//            My_hero = world.getMyHeroes()[0];
//        } else if (world.getMovePhaseNum() == 1) {
//            My_hero = world.getMyHeroes()[1];
//        } else if (world.getMovePhaseNum() == 2) {
//            My_hero = world.getMyHeroes()[2];
//        } else if (world.getMovePhaseNum() == 3) {
//            My_hero = world.getMyHeroes()[3];
//        } else if (world.getMovePhaseNum() == 4) {
//            if (get_Hero_By_Name(HeroName.SENTRY, world).getAbility(AbilityName.SENTRY_RAY).isReady()) {
//                My_hero = get_Hero_By_Name(HeroName.SENTRY, world);
//            } else {
//                My_hero = get_Hero_By_Name(HeroName.BLASTER, world);
//            }
//        } else {
//            My_hero = world.getMyHeroes()[random.nextInt(3)];
//        }

//        for (Hero hero : heroes) {
//            int i = 0;
//            if (!hero.getCurrentCell().isInObjectiveZone()) {
//                System.out.println(hero.getName());
//        if (!My_hero.getCurrentCell().isInObjectiveZone()){

//        }
//            }
//            if (we_are_in_top) {
//                if (((hero.getCurrentCell().getRow() + 1) < 31
//                        && !world.getMap().getCell(hero.getCurrentCell().getRow() + 1,
//                        hero.getCurrentCell().getColumn()).isWall())) {
//                    int j = 0;
//
//
//                    world.moveHero(hero, Direction.DOWN);
//                } else if (((hero.getCurrentCell().getColumn() > 0)
//                        && !world.getMap().getCell(hero.getCurrentCell().getRow(),
//                        hero.getCurrentCell().getColumn() - 1).isWall())) {
//
//                    world.moveHero(hero, Direction.LEFT);
//                } else if (((hero.getCurrentCell().getColumn() < 30)
//                        && !world.getMap().getCell(hero.getCurrentCell().getRow(),
//                        hero.getCurrentCell().getColumn() + 1).isWall())) {
//                    world.moveHero(hero, Direction.RIGHT);
//                } else {
//                    if (hero.getCurrentCell().isInObjectiveZone()) {
//
//                    } else {
//                        world.moveHero(hero, Direction.UP);
//                    }
//                }
//            } else {
//                if (((hero.getCurrentCell().getRow() - 1) > 0
//                        && !world.getMap().getCell(hero.getCurrentCell().getRow() - 1,
//                        hero.getCurrentCell().getColumn()).isWall())) {
//                    world.moveHero(hero, Direction.UP);
//                } else if (((hero.getCurrentCell().getColumn() > 0)
//                        && !world.getMap().getCell(hero.getCurrentCell().getRow(),
//                        hero.getCurrentCell().getColumn() - 1).isWall())) {
//                    world.moveHero(hero, Direction.LEFT);
//                } else if (((hero.getCurrentCell().getColumn() < 30)
//                        && !world.getMap().getCell(hero.getCurrentCell().getRow(),
//                        hero.getCurrentCell().getColumn() + 1).isWall())) {
//                    world.moveHero(hero, Direction.RIGHT);
//                } else {
//                    if (hero.getCurrentCell().isInObjectiveZone()) {
//
//                    } else {
//                        world.moveHero(hero, Direction.DOWN);
//                    }
//                }
//            }
//    }


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
                    for (Cell Opp_cell : Opp_cells) {
                        if (world.isInVision(hero_cell, Opp_cell)) {
                            world.castAbility(hero, AbilityName.SENTRY_RAY, Opp_cell);
                        } else if (hero.getAbility(AbilityName.SENTRY_ATTACK).isReady()) {
                            if (world.manhattanDistance(hero_cell, Opp_cell) <= hero.getAbility(AbilityName.SENTRY_ATTACK).getAreaOfEffect()) {
                                world.castAbility(hero, AbilityName.SENTRY_ATTACK, Opp_cell);
                            }
                        }
                    }
                } else {
                    for (Cell Opp_cell : Opp_cells) {
                        if (hero.getAbility(AbilityName.SENTRY_ATTACK).isReady()) {
                            if (world.manhattanDistance(hero_cell, Opp_cell)
                                    <= hero.getAbility(AbilityName.SENTRY_ATTACK).getAreaOfEffect()) {
                                world.castAbility(hero, AbilityName.SENTRY_ATTACK, Opp_cell);
                            }
                        }
//                        else if (world.getOppHero(Opp_cells.get(j)).getName().equals(HeroName.SENTRY)
//                                && world.isInVision(world.getOppHero(Opp_cells.get(j)).getCurrentCell(), hero_cell)) {
//                            if ()
//                            world.castAbility(hero, AbilityName.SENTRY_DODGE, );
//                        }
                    }

                }

            } else if (hero.getName().equals(HeroName.BLASTER)) {
                if (hero.getAbility(AbilityName.BLASTER_BOMB).isReady()) {
                    for (Cell Opp_cell : Opp_cells) {
                        if (world.manhattanDistance(hero_cell, Opp_cell) <=
                                hero.getAbility(AbilityName.BLASTER_BOMB).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.BLASTER_BOMB, Opp_cell);
                        } else if (hero.getAbility(AbilityName.BLASTER_ATTACK).isReady() &&
                                world.manhattanDistance(hero_cell, Opp_cell)
                                        <= hero.getAbility(AbilityName.BLASTER_ATTACK).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.BLASTER_ATTACK, Opp_cell);
                        }
                    }
                } else if (hero.getAbility(AbilityName.BLASTER_ATTACK).isReady()) {
                    for (Cell opp_cell : Opp_cells) {
                        if (world.manhattanDistance(hero_cell, opp_cell) <=
                                hero.getAbility(AbilityName.BLASTER_ATTACK).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.BLASTER_ATTACK, opp_cell);
                        }
                    }
                }
            } else if (hero.getName().equals(HeroName.GUARDIAN)) {
                if (hero.getAbility(AbilityName.GUARDIAN_ATTACK).isReady()) {
                    for (Cell Opp_cell : Opp_cells) {
                        if (world.manhattanDistance(hero_cell, Opp_cell) <=
                                hero.getAbility(AbilityName.GUARDIAN_ATTACK).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.GUARDIAN_ATTACK, Opp_cell);
                        }
                    }
                }
                if (hero.getAbility(AbilityName.GUARDIAN_FORTIFY).isReady()) {
                    for (Hero hero1 : world.getMyHeroes()) {
                        Cell des = hero1.getCurrentCell();
                        if (hero1.getCurrentHP() < hero1.getMaxHP() && world.manhattanDistance(hero_cell, des) <=
                                hero.getAbility(AbilityName.GUARDIAN_FORTIFY).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.GUARDIAN_FORTIFY, des);
                        }
                    }
                }
            } else {
                // Healer
                if (hero.getAbility(AbilityName.HEALER_ATTACK).isReady()) {
                    for (Cell Opp_cell : Opp_cells) {
                        if (world.manhattanDistance(hero_cell, Opp_cell) <=
                                hero.getAbility(AbilityName.HEALER_ATTACK).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.HEALER_ATTACK, Opp_cell);
                        }
                    }
                }
                if (hero.getAbility(AbilityName.HEALER_HEAL).isReady()) {
                    for (Hero hero1 : world.getMyHeroes()) {
                        Cell des = hero1.getCurrentCell();
                        if (hero1.getCurrentHP() < hero1.getMaxHP() && world.manhattanDistance(hero_cell, des) <=
                                hero.getAbility(AbilityName.HEALER_HEAL).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.HEALER_HEAL, des);
                        }
                    }
                }
            }

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

    public Cell change(Direction direction, World world, int row, int column) {
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

//    private Direction getDirection(Hero my_hero, World world, Direction offer) {
//        Cell current_cell = my_hero.getCurrentCell();
//        Direction direction = Direction.UP;
//        if (offer.compareTo(Direction.DOWN) == 0) {
//        } else if (offer.compareTo(Direction.UP) == 0) {
//
//        } else if (offer.compareTo(Direction.LEFT) == 0) {
//
//        } else {
//
//        }
//
//        if (we_are_in_top) {
//            if (current_cell.getRow() < 30) {
//                Cell down = world.getMap().getCell(current_cell.getRow() + 1, current_cell.getColumn());
//                if (!down.isWall() && !Not_Occupied_by_Ourselves(down, world.getMyHeroes())) {
//                    direction = Direction.DOWN;
//                }
//            } else if (current_cell.getColumn() < 30) {
//                Cell right = world.getMap().getCell(current_cell.getRow(), current_cell.getColumn() + 1);
//                if (!right.isWall() && !Not_Occupied_by_Ourselves(right, world.getMyHeroes())) {
//                    direction = Direction.RIGHT;
//                }
//            } else if (current_cell.getColumn() > 1) {
//                Cell left = world.getMap().getCell(current_cell.getRow(), current_cell.getColumn() - 1);
//                if (!left.isWall() && !Not_Occupied_by_Ourselves(left, world.getMyHeroes())) {
//                    direction = Direction.LEFT;
//                }
//            } else {
//                direction = Direction.UP;
//            }
//        } else {
//            if (current_cell.getRow() > 0) {
//                Cell down = world.getMap().getCell(current_cell.getRow() - 1, current_cell.getColumn());
//                if (!down.isWall() && !Not_Occupied_by_Ourselves(down, world.getMyHeroes())) {
//                    direction = Direction.UP;
//                }
//            } else if (current_cell.getColumn() < 30) {
//                Cell right = world.getMap().getCell(current_cell.getRow(), current_cell.getColumn() + 1);
//                if (!right.isWall() && !Not_Occupied_by_Ourselves(right, world.getMyHeroes())) {
//                    direction = Direction.RIGHT;
//                }
//            } else if (current_cell.getColumn() > 1) {
//                Cell left = world.getMap().getCell(current_cell.getRow(), current_cell.getColumn() + 1);
//                if (!left.isWall() && !Not_Occupied_by_Ourselves(left, world.getMyHeroes())) {
//                    direction = Direction.LEFT;
//                }
//            } else {
//                direction = Direction.DOWN;
//            }
//        }
//        return direction;
////    }
//
//    private boolean Not_Occupied_by_Ourselves(Cell down, Hero[] myHeroes) {
//        for (Hero hero : myHeroes) {
//            if (hero.getCurrentCell().equals(down)) {
//                return false;
//            }
//        }
//        return true;
//    }

//            for (int i = 0; i < 4; i++) {
//                if (Opp_cells[i] != null) {
//                    if (temp != null) {
//                        if (world.manhattanDistance(hero.getCurrentCell(), Opp_cells[i]) <
//                                world.manhattanDistance(hero.getCurrentCell(), temp)) {
//                            row = Opp_cells[i].getRow();
//                            column = Opp_cells[i].getColumn();
//                            temp = Opp_cells[i];
//                        }
//                    }
//                }
//            }
//            if (hero.getName().name().equals("Sentry")) {
//                if (hero.getAbility(AbilityName.SENTRY_RAY).isReady()) {
//                    world.castAbility(hero, hero.getAbility(AbilityName.SENTRY_RAY), row, column);
//                }
//            } else {
//                if (hero.getName() != HeroName.HEALER) {
//                    world.castAbility(hero, hero.getAbilities()[random.nextInt(3)], row, column);
//                } else {
//                    world.castAbility(hero, hero.getAbility(AbilityName.HEALER_HEAL), world.getMyHeroes()[random.nextInt(3)].getCurrentCell());
//                }
//            }

    private Cell is_under_attack(int i, Cell hero_cell, Cell cell, World world) {

        return null;
    }


    private Hero get_Hero_By_Name(HeroName heroName, World world) {
        for (Hero hero : world.getMyHeroes()) {
            if (hero.getName().equals(heroName)) {
                return hero;
            }
        }
        return null;
    }

}
