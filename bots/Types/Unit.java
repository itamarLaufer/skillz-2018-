package bots.Types;


import bots.Actions.Pushings;
import bots.Actions.Sails;
import bots.Data.BotData;
import bots.Data.Flags;
import bots.MapMethods;
import pirates.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * manages a dou of pirates that make a UNIT, when one of them is the carry-boy and one is helping him, especially pushing him away from being stuck
 * and into the mothership
 */

public class Unit {
    private BotData data;
    public static PirateGame game;
    public boolean offside;
    public List<Pirate> pirates; //list of all the pirates in the unit (contains 2)
    final static int SIZE = 2; // the unit size
    public boolean attackedThisTurn = false; // if the unit pushed this turn together - used when pushing a friend
    public Pirate leader;
    public List<Location> snakeLocations;
    public MapObject dest;

    public Unit(Pirate capsPirate) { //start a unit with the carry-boy. !called EVERY frame!
        data = BotData.getInstance();
        game = data.game;

        offside = data.isOffside(capsPirate);
        this.pirates = new ArrayList<>();
        this.pirates.add(capsPirate);
        leader = capsPirate;
        snakeLocations = new ArrayList<>();

        dest = MapMethods.getShorterRoute(leader, MapMethods.closest(leader, game.getMyMotherships()));
    }

    public void initializeSnakeLocations(int snakeLength) {

    }

    public boolean isFull() { //if we have enough pirates for a unit
        return pirates.size() >= SIZE;
    }

    public boolean contains(Pirate pirate) { //returns if the pirate is in the unit
        return pirates.contains(pirate);
    }

    // this method must be called be called ONCE a turn
    public void carefulSail(MapObject dest) { //send the carry-boy forward to the motherbase and his helper to follow him
        Sails.carefulSailOfUnits(this, dest);
    }

    public void sail(MapObject dest) {  //send all the pirates to the destination
        pirates.forEach(it -> it.sail(dest));
    }

    public boolean canPushEachOther() {
        return (pirates.get(0).canPush(pirates.get(1)) && pirates.get(1).canPush(pirates.get(0)));

    }

    public List<Pirate> getPiratesWithoutLeader() {
        return pirates.stream().filter(pirate -> !pirate.hasCapsule()).collect(Collectors.toList());
    }

    // returns how many turns the stray will take
    public boolean tryMemberStrayTowardsLeader() { //stray to fuhrer
        for (int i = 0; i < 25; i++) {
            Location strayLoc = MapMethods.getStrayTowardsCapsuleLoc(pirates.get(1), i);
            if (strayLoc != null) {
                pirates.get(1).sail(Sails.getNormalCarefulSailLoc(pirates.get(1), strayLoc));
                return true;
            }
        }

        return false;
    }

    public Pirate get(int i) {
        if (pirates.size() > i)
            return pirates.get(i);
        else
            return null;
    }

    public void remove(Pirate pirate) {
        pirates.remove(pirate);
    }

    public Pirate getLeader() {
        return leader;
    }

    public boolean tryPush() {
        if (Pushings.tryTicTacAttackPush(this))
            return true;

        int counter = 0;

        for (Pirate pirate : pirates)
            if (data.upgraded(pirate).acted || pirate != null && Pushings.tryPush(pirate))
                counter++;

        return counter > 0;

    }


    public void addPirate(Pirate pirate) {
        if (pirate == null)
            return;
        pirates.add(pirate);
    }

    public boolean hasAttackedThisTurn() {
        return attackedThisTurn;
    }

    public void setAttackedThisTurn(boolean attackedThisTurn) {
        this.attackedThisTurn = attackedThisTurn;
    }

    public List<Pirate> getPirates() {
        return this.pirates;
    }

    public boolean allLocationsAre(Location beforeCaps) {
        return pirates.stream().allMatch(pirate -> pirate.location.equals(beforeCaps));
    }


    //must be called before defenders did their turn
    public boolean swap2Fat2PushLeader(Location dest) {//we only call this if we didnt push and this is called once per unit therefor by now we know exactly where we'll be in the next turn

        if (getPirates().size() > 1) {
            //we know we didnt push and we know this is called once per unit therefor by now we know exactly where we'll be in the next turn
            Location nextLeaderLoc = Sails.getNormalCarefulSailLoc(leader, dest);
            Location nextFriendLoc = get(1).location.towards(leader, get(1).maxSpeed);
            Mothership closeMs = MapMethods.closest(nextLeaderLoc, game.getMyMotherships());
            boolean canPushNextTurn = nextFriendLoc.distance(nextLeaderLoc) < get(1).pushRange && get(1).pushReloadTurns <= 1;

            Location locationAfterPush = nextLeaderLoc.towards(closeMs, game.heavyPushDistance + leader.maxSpeed);//location in 2 turns

            // if the friend will be able to push leader next turn and the leader will be in an offside position
            if ((nextLeaderLoc.distance(closeMs) < game.heavyPushDistance + leader.maxSpeed + game.mothershipUnloadRange || data.isOffside(locationAfterPush, true) || true) && canPushNextTurn && get(1).stateName.equals(game.STATE_NAME_NORMAL) && !Flags.SWAPPED_THIS_TURN) {//switch with a defender
                for (Pirate atk : data.attackingPirates) {
                    if (atk.stateName.equals("heavy")) {
                        System.out.println("found a heavy attacking pirate, switching...");
                        atk.swapStates(get(1));
                        atk.pushReloadTurns = 10;
                        data.attackingPirates.remove(atk);
                        System.out.println(atk + " swaps with " + get(1) + " for longer push distance");
                        Flags.SWAPPED_THIS_TURN = true;

                        return true; //exit loop
                    }
                }
            }
        }
        return false;
    }

    // returns whether all the pirates can push the leader
    public boolean isTogether() {
        for (Pirate pirate : pirates)
            if (pirate.distance(leader) > pirate.pushRange)
                return false;

        return true;
    }

    // returns the middle point of all friendly pirates
    public Location getMiddleLocation() {
        int sumY = 0;
        int sumX = 0;

        for (Pirate p : pirates) {
            sumY += p.location.row;
            sumX += p.location.col;
            if (p.capsule != null) {
                sumY += p.location.row;
                sumX += p.location.col;
            }
        }
        return new Location(sumY / (pirates.size() + 1), sumX / (pirates.size() + 1));
    }

    public int size() {
        return pirates.size();
    }

}
