/*
 * This file is part of the vboard distribution.
 * (https://github.com/voyages-sncf-technologies/vboard)
 * Copyright (c) 2017 VSCT.
 *
 * vboard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, version 3.
 *
 * vboard is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.vsct.vboard.services;

import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class GamificationService {

    // Values to reach to obtain the corresponding level (arbitrary values)
    private static final int LEVEL_1 = 1;
    private static final int LEVEL_2 = 10;
    private static final int LEVEL_3 = 50;
    private static final int LEVEL_4 = 100;
    private static final int LEVEL_5 = 250;
    private static final int LEVEL_6 = 500;
    private static final int LEVEL_7 = 1000;
    private static final int LEVEL_8 = 2500;
    private static final int LEVEL_9 = 5000;
    private static final int LEVEL_10 = 10000;
    private final int LEVEL[] = {LEVEL_1, LEVEL_2, LEVEL_3, LEVEL_4, LEVEL_5, LEVEL_6, LEVEL_7, LEVEL_8, LEVEL_9, LEVEL_10};

    // Get the level corresponding to the points given
    public int getLevel(int points) {
        return this.getLevel(points, 10);
    }

    // Get the level corresponding to the points given
    public int getLevel(double points) {
        return this.getLevel((int)points, 10);
    }

    private int getLevel(int points, int level) {
        if (level <= 0){
            return 0;
        }
        if (points >= LEVEL[level-1]) {
            return level;
        } else {
            return this.getLevel(points, level-1);
        }
    }

    // Get the percentage already achieved to get to the next level
    public int getPercentage(double points) {
        int level = this.getLevel((int)points);
        if(level == 10) { return 100; }
        if(level == 0) { return (int)(points*100); }
        return (int)((points - LEVEL[level-1])/(double)(LEVEL[level] - LEVEL[level-1])*100);
    }

    // Return an array of the different level to get a badge (3: bronze, 5: argent, 7: or, 10: diamond)
    public ArrayList<Integer> levels() {
        ArrayList<Integer> levels = new ArrayList<>();
        levels.add(3);
        levels.add(5);
        levels.add(7);
        levels.add(10);
        return levels;
    }

    // Message for the different badge level (concerning the user)
    public String badgesMessageUser(int level) {
        switch (level) {
            case 3: return "amateur";
            case 5: return "acharné";
            case 7: return "d'or";
            case 10: return "absolu";
            default: return "";
        }
    }

    // Message for the different badge level (concerning an element)
    public String badgesMessageElement(int level) {
        switch (level) {
            case 3: return "bien";
            case 5: return "très";
            case 7: return "extrêment";
            case 10: return "incontestablement";
            default: return "";
        }
    }


}
