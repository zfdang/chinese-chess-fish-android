/*
    DroidFish - An Android chess program.
    Copyright (C) 2014-2016  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.petero.droidfish.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


public class UCIOptions {
    private ArrayList<String> names;
    private Map<String, OptionBase> options;

    public enum Type {
        CHECK,
        SPIN,
        COMBO,
        BUTTON,
        STRING
    }

    public abstract static class OptionBase{
        public String name;
        public Type type;
        public boolean visible = true; // True if visible in "Engine Options" dialog


        /**
         * Return true if current value != default value.
         */
        abstract public boolean modified();

        /**
         * Return current value as a string.
         */
        abstract public String getStringValue();

        /**
         * Set option from string value. Return true if option was modified.
         */
        public final boolean setFromString(String value) {
            OptionBase o = this;
            switch (o.type) {
                case CHECK:
                    if (value.toLowerCase(Locale.US).equals("true"))
                        return ((CheckOption) o).set(true);
                    else if (value.toLowerCase(Locale.US).equals("false"))
                        return ((CheckOption) o).set(false);
                    return false;
                case SPIN:
                    try {
                        int val = Integer.parseInt(value);
                        SpinOption so = (SpinOption) o;
                        return so.set(val);
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                case BUTTON:
                    return false;
                case STRING:
                    return ((StringOption) o).set(value);
            }
            return false;
        }
    }

    public static final class CheckOption extends OptionBase {
        private static final long serialVersionUID = 1L;
        public boolean value;
        public boolean defaultValue;

        CheckOption(String name, boolean def) {
            this.name = name;
            this.type = Type.CHECK;
            this.value = def;
            this.defaultValue = def;
        }

        @Override
        public boolean modified() {
            return value != defaultValue;
        }

        @Override
        public String getStringValue() {
            return value ? "true" : "false";
        }

        public boolean set(boolean value) {
            if (this.value != value) {
                this.value = value;
                return true;
            }
            return false;
        }
    }

    public static final class SpinOption extends OptionBase {
        private static final long serialVersionUID = 1L;
        public int minValue;
        public int maxValue;
        public int value;
        public int defaultValue;

        SpinOption(String name, int minV, int maxV, int def) {
            this.name = name;
            this.type = Type.SPIN;
            this.minValue = minV;
            this.maxValue = maxV;
            this.value = def;
            this.defaultValue = def;
        }

        @Override
        public boolean modified() {
            return value != defaultValue;
        }

        @Override
        public String getStringValue() {
            return String.format(Locale.US, "%d", value);
        }

        public boolean set(int value) {
            if ((value >= minValue) && (value <= maxValue)) {
                if (this.value != value) {
                    this.value = value;
                    return true;
                }
            }
            return false;
        }
    }

    public static final class ComboOption extends OptionBase {
        private static final long serialVersionUID = 1L;
        public String[] allowedValues;
        public String value;
        public String defaultValue;
        ComboOption(String name, String[] allowed, String def) {
            this.name = name;
            this.type = Type.COMBO;
            this.allowedValues = allowed;
            this.value = def;
            this.defaultValue = def;
        }
        @Override
        public boolean modified() {
            return !value.equals(defaultValue);
        }
        @Override
        public String getStringValue() {
            return value;
        }
        public boolean set(String value) {
            for (String allowed : allowedValues) {
                if (allowed.toLowerCase(Locale.US).equals(value.toLowerCase(Locale.US))) {
                    if (!this.value.equals(allowed)) {
                        this.value = allowed;
                        return true;
                    }
                    break;
                }
            }
            return false;
        }
    }

    public static final class ButtonOption extends OptionBase {
        private static final long serialVersionUID = 1L;
        public boolean trigger;

        ButtonOption(String name) {
            this.name = name;
            this.type = Type.BUTTON;
            this.trigger = false;
        }

        @Override
        public boolean modified() {
            return false;
        }

        @Override
        public String getStringValue() {
            return "";
        }
    }

    public static final class StringOption extends OptionBase {
        private static final long serialVersionUID = 1L;
        public String value;
        public String defaultValue;

        StringOption(String name, String def) {
            this.name = name;
            this.type = Type.STRING;
            this.value = def;
            this.defaultValue = def;
        }

        @Override
        public boolean modified() {
            return !value.equals(defaultValue);
        }

        @Override
        public String getStringValue() {
            return value;
        }

        public boolean set(String value) {
            if (!this.value.equals(value)) {
                this.value = value;
                return true;
            }
            return false;
        }
    }

    UCIOptions() {
        names = new ArrayList<>();
        options = new TreeMap<>();
        initialize();
    }

    // create initialize function to add all above default values
    public void initialize() {
        //      current all options for pikafish engine
        //
        //      id name Pikafish dev-20240822-nogit
        //      id author the Pikafish developers (see AUTHORS file)
        //
        //      option name Debug Log File type string default
        //      option name NumaPolicy type string default auto
        //      option name Threads type spin default 1 min 1 max 1024
        //      option name Hash type spin default 16 min 1 max 33554432
        //      option name Clear Hash type button
        //      option name Ponder type check default false
        //      option name MultiPV type spin default 1 min 1 max 128
        //      option name Move Overhead type spin default 10 min 0 max 5000
        //      option name nodestime type spin default 0 min 0 max 10000
        //      option name UCI_ShowWDL type check default false
        //      option name EvalFile type string default pikafish.nnue
        //      uciok
        addOption(new StringOption("Debug Log File", ""));
        addOption(new StringOption("NumaPolicy", "auto"));
        addOption(new SpinOption("Threads", 1, 1024, 1));
        addOption(new SpinOption("Hash", 1, 33554432, 16));
        addOption(new ButtonOption("Clear Hash"));
        addOption(new CheckOption("Ponder", false));
        addOption(new SpinOption("MultiPV", 1, 128, 1));
        addOption(new SpinOption("Move Overhead", 0, 5000, 10));
        addOption(new SpinOption("nodestime", 0, 10000, 0));
        addOption(new CheckOption("UCI_ShowWDL", false));
        addOption(new StringOption("EvalFile", "pikafish.nnue"));
    }

    public void clear() {
        names.clear();
        options.clear();

        initialize();
    }

    public boolean contains(String optName) {
        return getOption(optName) != null;
    }

    public final String[] getOptionNames() {
        return names.toArray(new String[0]);
    }

    public final OptionBase getOption(String name) {
        return options.get(name.toLowerCase(Locale.US));
    }

    final void addOption(OptionBase p) {
        String name = p.name.toLowerCase(Locale.US);
        names.add(name);
        options.put(name, p);
    }
}
