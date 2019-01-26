var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
/* Generated from Java with JSweet 2.0.0-SNAPSHOT - http://www.jsweet.org */
var bc19;
(function (bc19) {
    var Action = (function () {
        function Action(signal, signalRadius, logs, castleTalk) {
            this.signal = 0;
            this.signal_radius = 0;
            this.logs = null;
            this.castle_talk = 0;
            this.signal = signal;
            this.signal_radius = signalRadius;
            this.logs = logs;
            this.castle_talk = castleTalk;
        }
        return Action;
    }());
    bc19.Action = Action;
    Action["__class"] = "bc19.Action";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var BCException = (function (_super) {
        __extends(BCException, _super);
        function BCException(errorMessage) {
            var _this = _super.call(this, errorMessage) || this;
            _this.message = errorMessage;
            Object.setPrototypeOf(_this, BCException.prototype);
            return _this;
        }
        return BCException;
    }(Error));
    bc19.BCException = BCException;
    BCException["__class"] = "bc19.BCException";
    BCException["__interfaces"] = ["java.io.Serializable"];
})(bc19 || (bc19 = {}));
(function (bc19) {
    var BCAbstractRobot = (function () {
        function BCAbstractRobot() {
            this.SPECS = null;
            this.gameState = null;
            this.logs = null;
            this.__signal = 0;
            this.signalRadius = 0;
            this.__castleTalk = 0;
            this.me = null;
            this.id = 0;
            this.fuel = 0;
            this.karbonite = 0;
            this.lastOffer = null;
            this.map = null;
            this.karboniteMap = null;
            this.fuelMap = null;
            this.resetState();
        }
        BCAbstractRobot.prototype.setSpecs = function (specs) {
            this.SPECS = specs;
        };
        /*private*/ BCAbstractRobot.prototype.resetState = function () {
            this.logs = ([]);
            this.__signal = 0;
            this.signalRadius = 0;
            this.__castleTalk = 0;
        };
        BCAbstractRobot.prototype._do_turn = function (gameState) {
            this.gameState = gameState;
            this.id = gameState.id;
            this.karbonite = gameState.karbonite;
            this.fuel = gameState.fuel;
            this.lastOffer = gameState.last_offer;
            this.me = this.getRobot(this.id);
            if (this.me.turn === 1) {
                this.map = gameState.map;
                this.karboniteMap = gameState.karbonite_map;
                this.fuelMap = gameState.fuel_map;
            }
            var t = null;
            try {
                t = this.turn();
            }
            catch (e) {
                t = new bc19.ErrorAction(e, this.__signal, this.signalRadius, this.logs, this.__castleTalk);
            }
            ;
            if (t == null)
                t = new bc19.Action(this.__signal, this.signalRadius, this.logs, this.__castleTalk);
            t.signal = this.__signal;
            t.signal_radius = this.signalRadius;
            t.logs = this.logs;
            t.castle_talk = this.__castleTalk;
            this.resetState();
            return t;
        };
        /*private*/ BCAbstractRobot.prototype.checkOnMap = function (x, y) {
            return x >= 0 && x < this.gameState.shadow[0].length && y >= 0 && y < this.gameState.shadow.length;
        };
        BCAbstractRobot.prototype.log = function (message) {
            /* add */ (this.logs.push(message) > 0);
        };
        BCAbstractRobot.prototype.signal = function (value, radius) {
            var fuelNeeded = (Math.ceil(Math.sqrt(radius)) | 0);
            if (this.fuel < fuelNeeded)
                throw new bc19.BCException("Not enough fuel to signal given radius.");
            if (value < 0 || value >= Math.pow(2, this.SPECS.COMMUNICATION_BITS))
                throw new bc19.BCException("Invalid signal, must be within bit range.");
            if (radius > 2 * Math.pow(this.SPECS.MAX_BOARD_SIZE - 1, 2))
                throw new bc19.BCException("Signal radius is too big.");
            this.__signal = value;
            this.signalRadius = radius;
            this.fuel -= fuelNeeded;
        };
        BCAbstractRobot.prototype.castleTalk = function (value) {
            if (value < 0 || value >= Math.pow(2, this.SPECS.CASTLE_TALK_BITS))
                throw new bc19.BCException("Invalid castle talk, must be between 0 and 2^8.");
            this.__castleTalk = value;
        };
        BCAbstractRobot.prototype.proposeTrade = function (k, f) {
            if (this.me.unit !== this.SPECS.CASTLE)
                throw new bc19.BCException("Only castles can trade.");
            if (Math.abs(k) >= this.SPECS.MAX_TRADE || Math.abs(f) >= this.SPECS.MAX_TRADE)
                throw new bc19.BCException("Cannot trade over " + ('' + (this.SPECS.MAX_TRADE)) + " in a given turn.");
            return new bc19.TradeAction(f, k, this.__signal, this.signalRadius, this.logs, this.__castleTalk);
        };
        BCAbstractRobot.prototype.buildUnit = function (unit, dx, dy) {
            if (this.me.unit !== this.SPECS.PILGRIM && this.me.unit !== this.SPECS.CASTLE && this.me.unit !== this.SPECS.CHURCH)
                throw new bc19.BCException("This unit type cannot build.");
            if (this.me.unit === this.SPECS.PILGRIM && unit !== this.SPECS.CHURCH)
                throw new bc19.BCException("Pilgrims can only build churches.");
            if (this.me.unit !== this.SPECS.PILGRIM && unit === this.SPECS.CHURCH)
                throw new bc19.BCException("Only pilgrims can build churches.");
            if (dx < -1 || dy < -1 || dx > 1 || dy > 1)
                throw new bc19.BCException("Can only build in adjacent squares.");
            if (!this.checkOnMap(this.me.x + dx, this.me.y + dy))
                throw new bc19.BCException("Can\'t build units off of map.");
            if (this.gameState.shadow[this.me.y + dy][this.me.x + dx] !== 0)
                throw new bc19.BCException("Cannot build on occupied tile.");
            if (!this.map[this.me.y + dy][this.me.x + dx])
                throw new bc19.BCException("Cannot build onto impassable terrain.");
            if (this.karbonite < this.SPECS.UNITS[unit].CONSTRUCTION_KARBONITE || this.fuel < this.SPECS.UNITS[unit].CONSTRUCTION_FUEL)
                throw new bc19.BCException("Cannot afford to build specified unit.");
            return new bc19.BuildAction(unit, dx, dy, this.__signal, this.signalRadius, this.logs, this.__castleTalk);
        };
        BCAbstractRobot.prototype.move = function (dx, dy) {
            if (this.me.unit === this.SPECS.CASTLE || this.me.unit === this.SPECS.CHURCH)
                throw new bc19.BCException("Churches and Castles cannot move.");
            if (!this.checkOnMap(this.me.x + dx, this.me.y + dy))
                throw new bc19.BCException("Can\'t move off of map.");
            if (this.gameState.shadow[this.me.y + dy][this.me.x + dx] === -1)
                throw new bc19.BCException("Cannot move outside of vision range.");
            if (this.gameState.shadow[this.me.y + dy][this.me.x + dx] !== 0)
                throw new bc19.BCException("Cannot move onto occupied tile.");
            if (!this.map[this.me.y + dy][this.me.x + dx])
                throw new bc19.BCException("Cannot move onto impassable terrain.");
            var r = dx * dx + dy * dy;
            if (r > this.SPECS.UNITS[this.me.unit].SPEED)
                throw new bc19.BCException("Slow down, cowboy.  Tried to move faster than unit can.");
            if (this.fuel < r * this.SPECS.UNITS[this.me.unit].FUEL_PER_MOVE)
                throw new bc19.BCException("Not enough fuel to move at given speed.");
            return new bc19.MoveAction(dx, dy, this.__signal, this.signalRadius, this.logs, this.__castleTalk);
        };
        BCAbstractRobot.prototype.mine = function () {
            if (this.me.unit !== this.SPECS.PILGRIM)
                throw new bc19.BCException("Only Pilgrims can mine.");
            if (this.fuel < this.SPECS.MINE_FUEL_COST)
                throw new bc19.BCException("Not enough fuel to mine.");
            if (this.karboniteMap[this.me.y][this.me.x]) {
                if (this.me.karbonite >= this.SPECS.UNITS[this.SPECS.PILGRIM].KARBONITE_CAPACITY)
                    throw new bc19.BCException("Cannot mine, as at karbonite capacity.");
            }
            else if (this.fuelMap[this.me.y][this.me.x]) {
                if (this.me.fuel >= this.SPECS.UNITS[this.SPECS.PILGRIM].FUEL_CAPACITY)
                    throw new bc19.BCException("Cannot mine, as at fuel capacity.");
            }
            else
                throw new bc19.BCException("Cannot mine square without fuel or karbonite.");
            return new bc19.MineAction(this.__signal, this.signalRadius, this.logs, this.__castleTalk);
        };
        BCAbstractRobot.prototype.give = function (dx, dy, k, f) {
            if (dx > 1 || dx < -1 || dy > 1 || dy < -1 || (dx === 0 && dy === 0))
                throw new bc19.BCException("Can only give to adjacent squares.");
            if (!this.checkOnMap(this.me.x + dx, this.me.y + dy))
                throw new bc19.BCException("Can\'t give off of map.");
            if (this.gameState.shadow[this.me.y + dy][this.me.x + dx] <= 0)
                throw new bc19.BCException("Cannot give to empty square.");
            if (k < 0 || f < 0 || this.me.karbonite < k || this.me.fuel < f)
                throw new bc19.BCException("Do not have specified amount to give.");
            return new bc19.GiveAction(k, f, dx, dy, this.__signal, this.signalRadius, this.logs, this.__castleTalk);
        };
        BCAbstractRobot.prototype.attack = function (dx, dy) {
            if (this.me.unit === this.SPECS.CHURCH)
                throw new bc19.BCException("Churches cannot attack.");
            if (this.fuel < this.SPECS.UNITS[this.me.unit].ATTACK_FUEL_COST)
                throw new bc19.BCException("Not enough fuel to attack.");
            if (!this.checkOnMap(this.me.x + dx, this.me.y + dy))
                throw new bc19.BCException("Can\'t attack off of map.");
            if (this.gameState.shadow[this.me.y + dy][this.me.x + dx] === -1)
                throw new bc19.BCException("Cannot attack outside of vision range.");
            var r = dx * dx + dy * dy;
            if (r > this.SPECS.UNITS[this.me.unit].ATTACK_RADIUS[1] || r < this.SPECS.UNITS[this.me.unit].ATTACK_RADIUS[0])
                throw new bc19.BCException("Cannot attack outside of attack range.");
            return new bc19.AttackAction(dx, dy, this.__signal, this.signalRadius, this.logs, this.__castleTalk);
        };
        BCAbstractRobot.prototype.getRobot = function (id) {
            if (id <= 0)
                return null;
            for (var i = 0; i < this.gameState.visible.length; i++) {
                if (this.gameState.visible[i].id === id) {
                    return this.gameState.visible[i];
                }
            }
            ;
            return null;
        };
        BCAbstractRobot.prototype.isVisible = function (robot) {
            for (var x = 0; x < this.gameState.shadow[0].length; x++) {
                for (var y = 0; y < this.gameState.shadow.length; y++) {
                    if (robot.id === this.gameState.shadow[y][x])
                        return true;
                }
                ;
            }
            ;
            return false;
        };
        BCAbstractRobot.prototype.isRadioing = function (robot) {
            return robot.signal >= 0;
        };
        BCAbstractRobot.prototype.getVisibleRobotMap = function () {
            return this.gameState.shadow;
        };
        BCAbstractRobot.prototype.getPassableMap = function () {
            return this.map;
        };
        BCAbstractRobot.prototype.getKarboniteMap = function () {
            return this.karboniteMap;
        };
        BCAbstractRobot.prototype.getFuelMap = function () {
            return this.fuelMap;
        };
        BCAbstractRobot.prototype.getVisibleRobots = function () {
            return this.gameState.visible;
        };
        BCAbstractRobot.prototype.turn = function () {
            return null;
        };
        return BCAbstractRobot;
    }());
    bc19.BCAbstractRobot = BCAbstractRobot;
    BCAbstractRobot["__class"] = "bc19.BCAbstractRobot";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var MineAction = (function (_super) {
        __extends(MineAction, _super);
        function MineAction(signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.action = null;
            _this.action = "mine";
            return _this;
        }
        return MineAction;
    }(bc19.Action));
    bc19.MineAction = MineAction;
    MineAction["__class"] = "bc19.MineAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var ErrorAction = (function (_super) {
        __extends(ErrorAction, _super);
        function ErrorAction(error, signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.error = null;
            _this.error = error.message;
            return _this;
        }
        return ErrorAction;
    }(bc19.Action));
    bc19.ErrorAction = ErrorAction;
    ErrorAction["__class"] = "bc19.ErrorAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var GiveAction = (function (_super) {
        __extends(GiveAction, _super);
        function GiveAction(giveKarbonite, giveFuel, dx, dy, signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.action = null;
            _this.give_karbonite = 0;
            _this.give_fuel = 0;
            _this.dx = 0;
            _this.dy = 0;
            _this.action = "give";
            _this.give_karbonite = giveKarbonite;
            _this.give_fuel = giveFuel;
            _this.dx = dx;
            _this.dy = dy;
            return _this;
        }
        return GiveAction;
    }(bc19.Action));
    bc19.GiveAction = GiveAction;
    GiveAction["__class"] = "bc19.GiveAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var BuildAction = (function (_super) {
        __extends(BuildAction, _super);
        function BuildAction(buildUnit, dx, dy, signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.action = null;
            _this.build_unit = 0;
            _this.dx = 0;
            _this.dy = 0;
            _this.action = "build";
            _this.build_unit = buildUnit;
            _this.dx = dx;
            _this.dy = dy;
            return _this;
        }
        return BuildAction;
    }(bc19.Action));
    bc19.BuildAction = BuildAction;
    BuildAction["__class"] = "bc19.BuildAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var TradeAction = (function (_super) {
        __extends(TradeAction, _super);
        function TradeAction(trade_fuel, trade_karbonite, signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.action = null;
            _this.trade_fuel = 0;
            _this.trade_karbonite = 0;
            _this.action = "trade";
            _this.trade_fuel = trade_fuel;
            _this.trade_karbonite = trade_karbonite;
            return _this;
        }
        return TradeAction;
    }(bc19.Action));
    bc19.TradeAction = TradeAction;
    TradeAction["__class"] = "bc19.TradeAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var MoveAction = (function (_super) {
        __extends(MoveAction, _super);
        function MoveAction(dx, dy, signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.action = null;
            _this.dx = 0;
            _this.dy = 0;
            _this.action = "move";
            _this.dx = dx;
            _this.dy = dy;
            return _this;
        }
        return MoveAction;
    }(bc19.Action));
    bc19.MoveAction = MoveAction;
    MoveAction["__class"] = "bc19.MoveAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var AttackAction = (function (_super) {
        __extends(AttackAction, _super);
        function AttackAction(dx, dy, signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.action = null;
            _this.dx = 0;
            _this.dy = 0;
            _this.action = "attack";
            _this.dx = dx;
            _this.dy = dy;
            return _this;
        }
        return AttackAction;
    }(bc19.Action));
    bc19.AttackAction = AttackAction;
    AttackAction["__class"] = "bc19.AttackAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var MyRobot = (function (_super) {
        __extends(MyRobot, _super);
        function MyRobot() {
            var _this = _super.call(this) || this;
            /*private*/ _this.IMPASSABLE = -1;
            /*private*/ _this.PASSABLE = 0;
            /*private*/ _this.KARBONITE = 1;
            /*private*/ _this.FUEL = 2;
            /*private*/ _this.adjacentSpaces = [[0, 1], [-1, 1], [-1, 0], [-1, -1], [0, -1], [1, -1], [1, 0], [1, 1]];
            /*private*/ _this.ourDeadCastles = 0;
            /*private*/ _this.castleLocs = (function (dims) { var allocate = function (dims) { if (dims.length == 0) {
                return 0;
            }
            else {
                var array = [];
                for (var i = 0; i < dims[0]; i++) {
                    array.push(allocate(dims.slice(1)));
                }
                return array;
            } }; return allocate(dims); })([3, 2]);
            /*private*/ _this.enemyCastleLocs = (function (dims) { var allocate = function (dims) { if (dims.length == 0) {
                return 0;
            }
            else {
                var array = [];
                for (var i = 0; i < dims[0]; i++) {
                    array.push(allocate(dims.slice(1)));
                }
                return array;
            } }; return allocate(dims); })([3, 2]);
            /*private*/ _this.robs = new Array(6);
            /*private*/ _this.numFuelMines = 0;
            /*private*/ _this.numKarbMines = 0;
            /*private*/ _this.karbosInUse = ([]);
            /*private*/ _this.fuelsInUse = ([]);
            /*private*/ _this.currentPath = null;
            /*private*/ _this.attackPriority = [4, 5, 3, 0, 2, 1];
            _this.hRefl = false;
            _this.fullMap = null;
            _this.robotMap = null;
            _this.xorKey = 0;
            _this.numCastles = 0;
            _this.globalTurn = 0;
            _this.pilgrimLim = 0;
            _this.home = 0;
            _this.locInPath = 0;
            _this.castleDir = 0;
            _this.sideDir = 0;
            _this.arrived = false;
            _this.targetCastle = 0;
            return _this;
        }
        MyRobot.prototype.turn = function () {
            if (this.me.turn === 1) {
                for (var i = 0; i < 6; i++) {
                    this.robs[i] = ([]);
                }
                ;
                this.getFMap();
                this.hRefl = this.getReflDir();
                this.setXorKey();
            }
            else {
                this.globalTurn += 1;
            }
            this.robotMap = this.getVisibleRobotMap();
            switch ((this.me.unit)) {
                case 0:
                    return this.castle();
                case 1:
                    return this.church();
                case 2:
                    return this.pilgrim();
                case 3:
                    return this.crusader();
                case 4:
                    return this.prophet();
                case 5:
                    return this.preacher();
            }
            return null;
        };
        /*private*/ MyRobot.prototype.castle = function () {
            if (this.me.turn === 1) {
                this.globalTurn = 1;
                /* add */ (this.robs[0].push(this.me.id) > 0);
                {
                    var array122 = this.getVisibleRobots();
                    for (var index121 = 0; index121 < array122.length; index121++) {
                        var cast = array122[index121];
                        {
                            if (cast.team === this.me.team && cast.id !== this.me.id) {
                                /* add */ (this.robs[0].push(cast.id) > 0);
                            }
                        }
                    }
                }
                this.pilgrimLim = (Math.floor(Math.min(this.numFuelMines * 1.25, this.numFuelMines * 0.75 + this.numKarbMines)) | 0) - this.robs[0].length;
                if (this.robs[0].length > 1) {
                    this.castleTalk(this.me.x ^ (this.xorKey % 256));
                    for (var i = 1; i < this.robs[0].length; i++) {
                        var cast = this.getRobot(/* get */ this.robs[0][i]);
                        if (cast.turn === 1) {
                            this.castleLocs[i][0] = cast.castle_talk ^ (this.xorKey % 256);
                        }
                    }
                    ;
                }
                this.castleLocs[0] = [this.me.x, this.me.y];
                return null;
            }
            else if (this.me.turn === 2) {
                if (this.robs[0].length > 1) {
                    this.castleTalk(this.me.y ^ (this.xorKey % 256));
                    for (var i = 1; i < this.robs[0].length; i++) {
                        var cast = this.getRobot(/* get */ this.robs[0][i]);
                        if (cast.turn === 2) {
                            this.castleLocs[i][1] = cast.castle_talk ^ (this.xorKey % 256);
                        }
                        else {
                            this.castleLocs[i][0] = cast.castle_talk ^ (this.xorKey % 256);
                        }
                    }
                    ;
                }
            }
            else if (this.me.turn === 3) {
                if (this.robs[0].length > 1) {
                    for (var i = 1; i < this.robs[0].length; i++) {
                        var cast = this.getRobot(/* get */ this.robs[0][i]);
                        if (cast.turn === 2) {
                            this.castleLocs[i][1] = cast.castle_talk ^ (this.xorKey % 256);
                        }
                    }
                    ;
                }
                this.getEnemyCastleLocs();
            }
            else if (this.me.turn === 849) {
                var numBuild = this.robs[0].length + this.robs[1].length;
                if (this.numCastles === 1) {
                    this.signal(4096, (Math.floor(((this.fullMap.length * this.fullMap.length / numBuild | 0) / numBuild | 0)) | 0));
                }
                else {
                    this.signal(this.castleLocs[1][0] + this.castleLocs[1][1] * 64, (Math.floor(((this.fullMap.length * this.fullMap.length / numBuild | 0) / numBuild | 0)) | 0));
                }
            }
            else if (this.me.turn === 850) {
                var numBuild = this.robs[0].length + this.robs[1].length;
                if (this.numCastles <= 2) {
                    this.signal(4096, (Math.floor(((this.fullMap.length * this.fullMap.length / numBuild | 0) / numBuild | 0)) | 0));
                }
                else {
                    this.signal(this.castleLocs[2][0] + this.castleLocs[2][1] * 64, (Math.floor(((this.fullMap.length * this.fullMap.length / numBuild | 0) / numBuild | 0)) | 0));
                }
            }
            if (this.me.turn > 3) {
                for (var castNum = 0; castNum < this.robs[0].length; castNum++) {
                    var castle = this.getCastObj(castNum);
                    if (castNum !== 0 && castle == null) {
                        this.log("yup");
                        this.ourDeadCastles += 1;
                        /* remove */ this.robs[0].splice(castNum, 1);
                    }
                    var talk = castle.castle_talk;
                    if (talk >= 1 && talk <= 5) {
                        this.getNewUnit(talk);
                    }
                }
                ;
            }
            if (this.me.turn % 20 === 0) {
                this.log("Turn: " + this.me.turn + ". Pilgrim population: " + this.robs[2].length + ". Prophet population:  " + this.robs[4].length + ". Pilgrim limit: " + this.pilgrimLim + ".");
            }
            var atk = this.autoAttack();
            if (atk != null) {
                var loc_1 = this.availAdjSq([atk[0] > 0 ? 1 : (atk[0] < 0 ? -1 : 0), atk[1] > 0 ? 1 : (atk[1] < 0 ? -1 : 0)]);
                if (this.karbonite >= 30 && this.fuel >= 50 && this.getRobot(this.robotMap[this.me.y + atk[1]][this.me.x + atk[0]]).unit !== this.SPECS.PILGRIM && loc_1 != null) {
                    this.castleTalk(3);
                    return this.buildUnit(3, loc_1[0], loc_1[1]);
                }
                return this.attack(atk[0], atk[1]);
            }
            if (this.fuel < this.SPECS.UNITS[this.SPECS.PILGRIM].CONSTRUCTION_FUEL + this.robs[2].length + 2 || this.karbonite < this.SPECS.UNITS[this.SPECS.PILGRIM].CONSTRUCTION_KARBONITE) {
                return null;
            }
            if (this.robs[2].length >= this.pilgrimLim) {
                if (this.me.turn < 850 && this.fuel >= this.SPECS.UNITS[this.SPECS.PROPHET].CONSTRUCTION_FUEL + 2 + this.robs[2].length * 6 && this.karbonite >= this.SPECS.UNITS[this.SPECS.PROPHET].CONSTRUCTION_KARBONITE) {
                    var doit = void 0;
                    if (this.fuel >= this.SPECS.UNITS[this.SPECS.PROPHET].CONSTRUCTION_FUEL * (this.robs[0].length + this.robs[1].length) + 2 + this.robs[2].length * 6 && this.karbonite >= this.SPECS.UNITS[this.SPECS.PROPHET].CONSTRUCTION_KARBONITE * this.robs[0].length) {
                        doit = 0;
                    }
                    else {
                        doit = ((Math.random() * (this.robs[0].length + this.robs[1].length)) | 0);
                    }
                    if (doit === 0) {
                        var loc_2 = this.randomAdjSq();
                        if (loc_2 != null) {
                            this.castleTalk(4);
                            return this.buildUnit(4, loc_2[0], loc_2[1]);
                        }
                    }
                }
                return null;
            }
            var loc = this.randomAdjSq();
            if (loc != null) {
                this.castleTalk(2);
                return this.buildUnit(this.SPECS.PILGRIM, loc[0], loc[1]);
            }
            return null;
        };
        /*private*/ MyRobot.prototype.church = function () {
            return null;
        };
        /*private*/ MyRobot.prototype.pilgrim = function () {
            if (this.me.turn === 1) {
                this.getHomeCastle();
                this.getEnemyCastleLocs();
                this.pilgrimLim = (Math.floor(Math.min(this.numFuelMines * 1.25, this.numFuelMines * 0.75 + this.numKarbMines)) | 0);
            }
            var castle = null;
            for (var dx_1 = -1; dx_1 <= 1; dx_1++) {
                var testX = this.me.x + dx_1;
                if (testX <= -1 || testX >= this.fullMap.length) {
                    continue;
                }
                for (var dy_1 = -1; dy_1 <= 1; dy_1++) {
                    var testY = this.me.y + dy_1;
                    if (testY <= -1 || testY >= this.fullMap.length) {
                        continue;
                    }
                    var maybe = this.getRobot(this.robotMap[testY][testX]);
                    if (this.robotMap[testY][testX] > 0 && maybe.unit === this.SPECS.CASTLE && maybe.team === this.me.team) {
                        castle = maybe;
                        /* clear */ (this.karbosInUse.length = 0);
                        /* clear */ (this.fuelsInUse.length = 0);
                    }
                }
                ;
            }
            ;
            if (this.currentPath != null && this.currentPath.length > this.locInPath) {
                var nextMove_1 = this.currentPath[this.locInPath];
                if (this.robotMap[nextMove_1[1]][nextMove_1[0]] <= 0) {
                    var dx_2 = nextMove_1[0] - this.me.x;
                    var dy_2 = nextMove_1[1] - this.me.y;
                    if (this.fuel >= (dx_2 * dx_2 + dy_2 * dy_2) * this.SPECS.UNITS[this.SPECS.PILGRIM].FUEL_PER_MOVE + this.pilgrimLim * 0.7) {
                        this.locInPath += 1;
                        return this.move(dx_2, dy_2);
                    }
                }
            }
            if (this.me.karbonite === this.SPECS.UNITS[this.SPECS.PILGRIM].KARBONITE_CAPACITY || this.me.fuel === this.SPECS.UNITS[this.SPECS.PILGRIM].FUEL_CAPACITY) {
                if (castle != null) {
                    return this.give(castle.x - this.me.x, castle.y - this.me.y, this.me.karbonite, this.me.fuel);
                }
                this.currentPath = this.bfs(this.castleLocs[this.home][0], this.castleLocs[this.home][1]);
                if (this.currentPath == null) {
                    this.log("Pilgrim BFS returned null. Turn: " + this.globalTurn);
                    if (this.fuel >= this.pilgrimLim) {
                        var move = this.randomAdjSq();
                        return this.move(move[0], move[1]);
                    }
                    else {
                        return null;
                    }
                }
                var nextMove_2 = this.currentPath[0];
                var dx_3 = nextMove_2[0] - this.me.x;
                var dy_3 = nextMove_2[1] - this.me.y;
                if (this.fuel >= (dx_3 * dx_3 + dy_3 * dy_3) * this.SPECS.UNITS[this.SPECS.PILGRIM].FUEL_PER_MOVE + this.pilgrimLim * 0.7) {
                    this.locInPath += 1;
                    return this.move(dx_3, dy_3);
                }
                return null;
            }
            if (this.fullMap[this.me.y][this.me.x] === this.KARBONITE || this.fullMap[this.me.y][this.me.x] === this.FUEL) {
                if (this.fuel === 0) {
                    this.log("can\'t mine b/c no fuel :\'(");
                    return null;
                }
                return this.mine();
            }
            var location;
            if (20 * this.pilgrimLim > this.fuel) {
                location = this.findClosestFuel();
            }
            else {
                location = this.findClosestKarbo();
            }
            if (location == null) {
                location = this.castleLocs[this.home];
            }
            this.currentPath = this.bfs(location[0], location[1]);
            if (this.currentPath == null) {
                this.log("Pilgrim BFS returned null (loc. 2). Turn: " + this.globalTurn);
                return null;
            }
            var nextMove = this.currentPath[this.locInPath];
            var dx = nextMove[0] - this.me.x;
            var dy = nextMove[1] - this.me.y;
            if (this.fuel >= (dx * dx + dy * dy) * this.SPECS.UNITS[this.SPECS.PILGRIM].FUEL_PER_MOVE + 5) {
                this.locInPath += 1;
                return this.move(dx, dy);
            }
            return null;
        };
        /*private*/ MyRobot.prototype.crusader = function () {
            if (this.me.turn === 1) {
                this.pilgrimLim = (Math.floor(Math.min(this.numFuelMines * 1.25, this.numFuelMines * 0.75 + this.numKarbMines)) | 0);
                this.getHomeCastle();
                this.getCastleDir();
                if (this.castleDir % 2 === 0) {
                    this.sideDir = ((((Math.random() * 2) | 0)) * 4 + this.castleDir + 2) % 8;
                }
            }
            var atk = this.autoAttack();
            if (atk != null) {
                if (this.fuel >= 10) {
                    return this.attack(atk[0], atk[1]);
                }
                else {
                    return null;
                }
            }
            this.getCastleLocs();
            if (this.globalTurn >= 850) {
                this.updateTargetCastle();
                if (this.currentPath == null || this.currentPath.length <= this.locInPath || this.robotMap[this.currentPath[this.locInPath][1]][this.currentPath[this.locInPath][0]] > 0 || this.globalTurn === 850) {
                    this.currentPath = this.bfs(this.enemyCastleLocs[this.targetCastle][0], this.enemyCastleLocs[this.targetCastle][1]);
                }
                if (this.currentPath == null || this.currentPath.length <= this.locInPath || this.robotMap[this.currentPath[this.locInPath][1]][this.currentPath[this.locInPath][0]] > 0) {
                    this.log("Prophet BFS returned null (or something invalid). Turn: " + this.globalTurn);
                    if (this.fuel >= this.pilgrimLim * 2) {
                        var mov_1 = this.randomAdjSq();
                        if (mov_1 != null) {
                            return this.move(mov_1[0], mov_1[1]);
                        }
                        return null;
                    }
                    else {
                        return null;
                    }
                }
                var mov = [/* get */ this.currentPath[this.locInPath][0] - this.me.x, /* get */ this.currentPath[this.locInPath][1] - this.me.y];
                if (this.fuel >= (mov[0] * mov[0] + mov[1] * mov[1]) * 2 + this.pilgrimLim * 0.7) {
                    this.locInPath += 1;
                    return this.move(mov[0], mov[1]);
                }
                else {
                    return null;
                }
            }
            else {
                if (this.fuel >= this.pilgrimLim * 2) {
                    if (this.moveAway()) {
                        var mov = this.exploreLattice();
                        if (mov != null) {
                            return this.move(mov[0], mov[1]);
                        }
                    }
                }
            }
            return null;
        };
        /*private*/ MyRobot.prototype.prophet = function () {
            if (this.me.turn === 1) {
                this.pilgrimLim = (Math.floor(Math.min(this.numFuelMines * 1.25, this.numFuelMines * 0.75 + this.numKarbMines)) | 0);
                this.getHomeCastle();
                this.arrived = false;
            }
            this.getCastleLocs();
            var atk = this.autoAttack();
            if (atk != null) {
                return this.attack(atk[0], atk[1]);
            }
            if (this.globalTurn >= 850) {
                this.updateTargetCastle();
                if (this.currentPath == null || this.currentPath.length <= this.locInPath || this.robotMap[this.currentPath[this.locInPath][1]][this.currentPath[this.locInPath][0]] > 0 || this.globalTurn === 850) {
                    this.currentPath = this.bfs(this.enemyCastleLocs[this.targetCastle][0], this.enemyCastleLocs[this.targetCastle][1]);
                }
                if (this.currentPath == null || this.currentPath.length <= this.locInPath || this.robotMap[this.currentPath[this.locInPath][1]][this.currentPath[this.locInPath][0]] > 0) {
                    this.log("Prophet BFS returned null (or something invalid). Turn: " + this.globalTurn);
                    if (this.fuel >= this.pilgrimLim * 2) {
                        var mov_2 = this.randomAdjSq();
                        if (mov_2 != null) {
                            return this.move(mov_2[0], mov_2[1]);
                        }
                        return null;
                    }
                    else {
                        return null;
                    }
                }
                var mov = [/* get */ this.currentPath[this.locInPath][0] - this.me.x, /* get */ this.currentPath[this.locInPath][1] - this.me.y];
                if (this.fuel >= (mov[0] * mov[0] + mov[1] * mov[1]) * 2 + this.pilgrimLim * 0.7) {
                    this.locInPath += 1;
                    return this.move(mov[0], mov[1]);
                }
                else {
                    return null;
                }
            }
            else {
                if (!this.arrived && this.fuel >= this.pilgrimLim * 2) {
                    if (this.currentPath == null || this.currentPath.length <= this.locInPath || this.robotMap[this.currentPath[this.locInPath][1]][this.currentPath[this.locInPath][0]] > 0) {
                        this.currentPath = this.goToLattice();
                    }
                    if (this.currentPath == null || this.currentPath.length <= this.locInPath || this.robotMap[this.currentPath[this.locInPath][1]][this.currentPath[this.locInPath][0]] > 0) {
                        this.log("Prophet BFS returned null (or something invalid). Turn: " + this.globalTurn);
                        var mov_3 = this.randomAdjSq();
                        if (mov_3 != null) {
                            if ((this.me.x + mov_3[0] + this.me.y + mov_3[1]) % 2 === 0) {
                                this.arrived = true;
                            }
                            return this.move(mov_3[0], mov_3[1]);
                        }
                        return null;
                    }
                    var mov = [/* get */ this.currentPath[this.locInPath][0] - this.me.x, /* get */ this.currentPath[this.locInPath][1] - this.me.y];
                    this.locInPath += 1;
                    if ((this.me.x + mov[0] + this.me.y + mov[1]) % 2 === 0 && !this.isNextToHome(this.me.x + mov[0], this.me.y + mov[1])) {
                        this.arrived = true;
                    }
                    return this.move(mov[0], mov[1]);
                }
            }
            return null;
        };
        /*private*/ MyRobot.prototype.preacher = function () {
            if (this.me.turn === 1) {
                this.pilgrimLim = (Math.floor(Math.min(this.numFuelMines * 1.25, this.numFuelMines * 0.75 + this.numKarbMines)) | 0);
                this.getCastleDir();
                this.getHomeCastle();
                if (this.castleDir % 2 === 0) {
                    this.sideDir = ((((Math.random() * 2) | 0)) * 4 + this.castleDir + 2) % 8;
                }
            }
            var atk = this.preacherAttack();
            if (atk != null) {
                return atk;
            }
            this.getCastleLocs();
            return null;
        };
        /*private*/ MyRobot.prototype.getFMap = function () {
            var m = this.getPassableMap();
            var k = this.getKarboniteMap();
            var f = this.getFuelMap();
            this.fullMap = (function (dims) { var allocate = function (dims) { if (dims.length == 0) {
                return 0;
            }
            else {
                var array = [];
                for (var i = 0; i < dims[0]; i++) {
                    array.push(allocate(dims.slice(1)));
                }
                return array;
            } }; return allocate(dims); })([m.length, m.length]);
            var h = m.length;
            var w = h;
            for (var i = 0; i < h; i++) {
                for (var j = 0; j < w; j++) {
                    if (!m[i][j]) {
                        this.fullMap[i][j] = this.IMPASSABLE;
                    }
                    else if (k[i][j]) {
                        this.numKarbMines++;
                        this.fullMap[i][j] = this.KARBONITE;
                    }
                    else if (f[i][j]) {
                        this.numFuelMines++;
                        this.fullMap[i][j] = this.FUEL;
                    }
                    else {
                        this.fullMap[i][j] = this.PASSABLE;
                    }
                }
                ;
            }
            ;
        };
        /*private*/ MyRobot.prototype.getReflDir = function () {
            var top = ((this.fullMap.length + 1) / 2 | 0);
            var left = ((this.fullMap[0].length + 1) / 2 | 0);
            for (var i = 0; i < top; i++) {
                for (var j = 0; j < left; j++) {
                    if (this.fullMap[i][j] !== this.fullMap[this.fullMap.length - 1 - i][j]) {
                        return true;
                    }
                    else if (this.fullMap[i][j] !== this.fullMap[i][this.fullMap[0].length - 1 - j]) {
                        return false;
                    }
                }
                ;
            }
            ;
            for (var i = this.fullMap.length; i > top; i--) {
                for (var j = this.fullMap[0].length; j > left; j--) {
                    if (this.fullMap[i][j] !== this.fullMap[this.fullMap.length - 1 - i][j]) {
                        return true;
                    }
                    else if (this.fullMap[i][j] !== this.fullMap[i][this.fullMap[0].length - 1 - j]) {
                        return false;
                    }
                }
                ;
            }
            ;
            this.log("it\'s frickin reflected both ways >:(");
            return true;
        };
        /*private*/ MyRobot.prototype.getHomeCastle = function () {
            {
                var array124 = this.getVisibleRobots();
                for (var index123 = 0; index123 < array124.length; index123++) {
                    var rob = array124[index123];
                    {
                        if (rob.unit === this.SPECS.CASTLE) {
                            this.castleLocs[0] = [rob.x, rob.y];
                            /* add */ (this.robs[0].push(rob.id) > 0);
                            this.globalTurn = rob.turn;
                        }
                    }
                }
            }
        };
        /*private*/ MyRobot.prototype.findClosestKarbo = function () {
            var minDistance = this.fullMap.length * this.fullMap.length;
            var ans;
            for (var x = 0; x < this.fullMap[0].length; x++) {
                looping: for (var y = 0; y < this.fullMap.length; y++) {
                    if (this.fullMap[y][x] === this.KARBONITE) {
                        var temp = [x, y];
                        var _loop_1 = function (index125) {
                            var out = this_1.karbosInUse[index125];
                            {
                                if (out[0] === temp[0] && out[1] === temp[1]) {
                                    if (this_1.robotMap[y][x] === 0) {
                                        /* remove */ (function (a) { return a.splice(a.indexOf(out), 1); })(this_1.karbosInUse);
                                    }
                                    else {
                                        return "continue-looping";
                                    }
                                }
                            }
                        };
                        var this_1 = this;
                        for (var index125 = 0; index125 < this.karbosInUse.length; index125++) {
                            var state_1 = _loop_1(index125);
                            switch (state_1) {
                                case "continue-looping": continue looping;
                            }
                        }
                        if (this.robotMap[y][x] > 0) {
                            /* add */ (this.karbosInUse.push(temp) > 0);
                            continue looping;
                        }
                        var dx = x - this.me.x;
                        var dy = y - this.me.y;
                        if (dx * dx + dy * dy < minDistance) {
                            ans = temp;
                            minDistance = dx * dx + dy * dy;
                        }
                    }
                }
                ;
            }
            ;
            return ans;
        };
        /*private*/ MyRobot.prototype.findClosestFuel = function () {
            var minDistance = this.fullMap.length * this.fullMap.length;
            var ans = [0, 0];
            for (var x = 0; x < this.fullMap[0].length; x++) {
                looping: for (var y = 0; y < this.fullMap.length; y++) {
                    if (this.fullMap[y][x] === this.FUEL) {
                        var temp = [x, y];
                        var _loop_2 = function (index126) {
                            var out = this_2.fuelsInUse[index126];
                            {
                                if (out[0] === temp[0] && out[1] === temp[1]) {
                                    if (this_2.robotMap[y][x] === 0) {
                                        /* remove */ (function (a) { return a.splice(a.indexOf(out), 1); })(this_2.fuelsInUse);
                                    }
                                    else {
                                        return "continue-looping";
                                    }
                                }
                            }
                        };
                        var this_2 = this;
                        for (var index126 = 0; index126 < this.fuelsInUse.length; index126++) {
                            var state_2 = _loop_2(index126);
                            switch (state_2) {
                                case "continue-looping": continue looping;
                            }
                        }
                        if (this.robotMap[y][x] > 0) {
                            /* add */ (this.fuelsInUse.push(temp) > 0);
                            continue looping;
                        }
                        var dx = x - this.me.x;
                        var dy = y - this.me.y;
                        if (dx * dx + dy * dy < minDistance) {
                            ans = temp;
                            minDistance = dx * dx + dy * dy;
                        }
                    }
                }
                ;
            }
            ;
            return ans;
        };
        /*private*/ MyRobot.prototype.setXorKey = function () {
            var parts = [0, 0, 0, 0];
            parts[0] = 5 + this.fullMap[9][30] + this.fullMap[18][8] + this.fullMap[9][0] + this.fullMap[23][28] + this.fullMap[15][31];
            parts[1] = 5 + this.fullMap[19][3] + this.fullMap[31][8] + this.fullMap[10][26] + this.fullMap[11][11] + this.fullMap[4][2];
            parts[2] = 5 + this.fullMap[6][9] + this.fullMap[4][20] + this.fullMap[13][3] + this.fullMap[18][29] + this.fullMap[19][12];
            parts[3] = 5 + this.fullMap[30][10] + this.fullMap[31][31] + this.fullMap[0][0] + this.fullMap[5][15] + this.fullMap[1][8];
            this.xorKey = parts[3] * 4096 + parts[2] * 256 + parts[1] * 16 + parts[0];
        };
        /*private*/ MyRobot.prototype.getEnemyCastleLocs = function () {
            for (var i = 0; i < 3; i++) {
                if (this.hRefl) {
                    this.enemyCastleLocs[i][0] = this.fullMap.length - 1 - this.castleLocs[i][0];
                    this.enemyCastleLocs[i][1] = this.castleLocs[i][1];
                }
                else {
                    this.enemyCastleLocs[i][0] = this.castleLocs[i][0];
                    this.enemyCastleLocs[i][1] = this.fullMap.length - 1 - this.castleLocs[i][1];
                }
            }
            ;
        };
        /*private*/ MyRobot.prototype.getTargetCastle = function () {
            if (this.numCastles === 1) {
                this.targetCastle = 0;
            }
            else if (this.numCastles === 2) {
                this.targetCastle = this.fullMap[17][22] === 0 ? 0 : 1;
            }
            else if (this.numCastles === 3) {
                var minInd = void 0;
                var maxInd = void 0;
                var min = 64;
                var max = -1;
                for (var i = 0; i < 3; i++) {
                    if (this.enemyCastleLocs[i][this.hRefl ? 1 : 0] < min) {
                        minInd = i;
                        min = this.enemyCastleLocs[i][this.hRefl ? 1 : 0];
                    }
                    if (this.enemyCastleLocs[i][this.hRefl ? 1 : 0] > max) {
                        maxInd = i;
                        max = this.enemyCastleLocs[i][this.hRefl ? 1 : 0];
                    }
                }
                ;
                this.enemyCastleLocs = [this.enemyCastleLocs[minInd], this.enemyCastleLocs[3 - minInd - maxInd], this.enemyCastleLocs[maxInd]];
                this.targetCastle = this.fullMap[22][17] === 0 ? 2 : 0;
            }
            else {
                this.log("uh oh numCastles is " + this.numCastles);
            }
        };
        /*private*/ MyRobot.prototype.getEnemiesInRange = function () {
            var robs = this.getVisibleRobots();
            var enms = ([]);
            for (var index127 = 0; index127 < robs.length; index127++) {
                var rob = robs[index127];
                {
                    if (rob.team !== this.me.team && (rob.x - this.me.x) * (rob.x - this.me.x) + (rob.y - this.me.y) * (rob.y - this.me.y) <= this.SPECS.UNITS[this.me.unit].ATTACK_RADIUS[1] && (rob.x - this.me.x) * (rob.x - this.me.x) + (rob.y - this.me.y) * (rob.y - this.me.y) >= this.SPECS.UNITS[this.me.unit].ATTACK_RADIUS[0]) {
                        /* add */ (enms.push(rob) > 0);
                    }
                }
            }
            return enms.slice(0);
        };
        /*private*/ MyRobot.prototype.autoAttack = function () {
            if (this.fuel < this.SPECS.UNITS[this.me.unit].ATTACK_FUEL_COST) {
                return null;
            }
            var robs = this.getEnemiesInRange();
            if (robs.length === 0) {
                return null;
            }
            var priorRobs = ([]);
            var found = false;
            var i = 0;
            while ((!found && i < 6)) {
                for (var index128 = 0; index128 < robs.length; index128++) {
                    var rob = robs[index128];
                    {
                        if (rob.unit === this.attackPriority[i]) {
                            found = true;
                            /* add */ (priorRobs.push(rob) > 0);
                        }
                    }
                }
                i++;
            }
            ;
            if (priorRobs.length === 1) {
                return [/* get */ priorRobs[0].x - this.me.x, /* get */ priorRobs[0].y - this.me.y];
            }
            else if (priorRobs.length === 0) {
                this.log("why are there no enemies and yet autoAttack() has gotten all the way here");
                return null;
            }
            var lowestID = 4097;
            for (var j = 0; j < priorRobs.length; j++) {
                if (priorRobs[j].id < lowestID) {
                    lowestID = priorRobs[j].id;
                }
            }
            ;
            return [this.getRobot(lowestID).x - this.me.x, this.getRobot(lowestID).y - this.me.y];
        };
        /*private*/ MyRobot.prototype.getPreacherKillableRobots = function () {
            var robs = this.getVisibleRobots();
            var killable = ([]);
            for (var index129 = 0; index129 < robs.length; index129++) {
                var rob = robs[index129];
                {
                    if (rob.team !== this.me.team && (rob.unit === this.SPECS.PILGRIM || rob.unit === this.SPECS.PROPHET)) {
                        /* add */ (killable.push(rob) > 0);
                    }
                }
            }
            return killable.slice(0);
        };
        /*private*/ MyRobot.prototype.getAllies = function () {
            var robs = this.getVisibleRobots();
            var allies = ([]);
            for (var index130 = 0; index130 < robs.length; index130++) {
                var rob = robs[index130];
                {
                    if (rob.team === this.me.team) {
                        /* add */ (allies.push(rob) > 0);
                    }
                }
            }
            return allies.slice(0);
        };
        /*private*/ MyRobot.prototype.getEnemyRobots = function () {
            var robs = this.getVisibleRobots();
            var enemies = ([]);
            for (var index131 = 0; index131 < robs.length; index131++) {
                var rob = robs[index131];
                {
                    if (rob.team !== this.me.team && (rob.unit === this.SPECS.CRUSADER || rob.unit === this.SPECS.PREACHER || rob.unit === this.SPECS.CASTLE)) {
                        /* add */ (enemies.push(rob) > 0);
                    }
                }
            }
            return enemies.slice(0);
        };
        /*private*/ MyRobot.prototype.getEnemyChurches = function () {
            var robs = this.getVisibleRobots();
            var buildings = ([]);
            for (var index132 = 0; index132 < robs.length; index132++) {
                var rob = robs[index132];
                {
                    if (rob.team !== this.me.team && rob.unit === this.SPECS.CHURCH) {
                        /* add */ (buildings.push(rob) > 0);
                    }
                }
            }
            return buildings.slice(0);
        };
        /*private*/ MyRobot.prototype.squareContainsRobot = function (rob, centerX, centerY) {
            if (rob.x + 1 >= centerX && rob.x - 1 <= centerX && rob.y + 1 >= centerY && rob.y - 1 <= centerY) {
                return true;
            }
            return false;
        };
        MyRobot.prototype.preacherAttack = function () {
            if (this.fuel < this.SPECS.UNITS[5].ATTACK_FUEL_COST) {
                return null;
            }
            var killable = this.getPreacherKillableRobots();
            var allies = this.getAllies();
            var attackLocs = (function (dims) { var allocate = function (dims) { if (dims.length == 0) {
                return 0;
            }
            else {
                var array = [];
                for (var i = 0; i < dims[0]; i++) {
                    array.push(allocate(dims.slice(1)));
                }
                return array;
            } }; return allocate(dims); })([9, 9]);
            var bestLocs = ([]);
            /* add */ (bestLocs.push([0, 0, 0]) > 0);
            for (var y = 0; y < 9; y++) {
                for (var x = 0; x < 9; x++) {
                    if (x === 3 && y > 2 && y < 6) {
                        x += 3;
                    }
                    attackLocs[y][x] = 0;
                    for (var index133 = 0; index133 < allies.length; index133++) {
                        var ally = allies[index133];
                        {
                            if (this.squareContainsRobot(ally, x, y)) {
                                attackLocs[y][x] = -100;
                            }
                        }
                    }
                    if (attackLocs[y][x] === 0) {
                        for (var index134 = 0; index134 < killable.length; index134++) {
                            var deathable = killable[index134];
                            {
                                if (this.squareContainsRobot(deathable, x, y)) {
                                    attackLocs[y][x] += 1;
                                }
                            }
                        }
                    }
                    if (attackLocs[y][x] > bestLocs[0][2]) {
                        /* clear */ (bestLocs.length = 0);
                        /* add */ (bestLocs.push([x, y, attackLocs[y][x]]) > 0);
                    }
                    else if (attackLocs[y][x] === bestLocs[0][2]) {
                        /* add */ (bestLocs.push([x, y, attackLocs[y][x]]) > 0);
                    }
                }
                ;
            }
            ;
            if (bestLocs[0][2] === 0) {
                return null;
            }
            else if (bestLocs.length === 1) {
                return this.attack(/* get */ bestLocs[0][0] - 4, /* get */ bestLocs[0][1] - 4);
            }
            var combat = this.getEnemyRobots();
            var bestbestLocs = ([]);
            /* add */ (bestbestLocs.push([0, 0, -1]) > 0);
            for (var index135 = 0; index135 < bestLocs.length; index135++) {
                var pos = bestLocs[index135];
                {
                    for (var index136 = 0; index136 < combat.length; index136++) {
                        var rob = combat[index136];
                        {
                            if (this.squareContainsRobot(rob, pos[0], pos[1])) {
                                attackLocs[pos[1]][pos[0]] += 1;
                            }
                        }
                    }
                    if (attackLocs[pos[1]][pos[0]] > bestbestLocs[0][2]) {
                        /* clear */ (bestbestLocs.length = 0);
                        /* add */ (bestbestLocs.push([pos[0], pos[1], attackLocs[pos[1]][pos[0]]]) > 0);
                    }
                    else if (attackLocs[pos[1]][pos[0]] === bestbestLocs[0][2]) {
                        /* add */ (bestbestLocs.push([pos[0], pos[1], attackLocs[pos[1]][pos[0]]]) > 0);
                    }
                }
            }
            if (bestbestLocs.length === 1) {
                return this.attack(/* get */ bestbestLocs[0][0] - 4, /* get */ bestbestLocs[0][1] - 4);
            }
            var build = this.getEnemyChurches();
            var goodLocs = ([]);
            /* add */ (goodLocs.push([0, 0, -1]) > 0);
            for (var index137 = 0; index137 < bestbestLocs.length; index137++) {
                var pos = bestbestLocs[index137];
                {
                    for (var index138 = 0; index138 < build.length; index138++) {
                        var rob = build[index138];
                        {
                            if (this.squareContainsRobot(rob, pos[0], pos[1])) {
                                attackLocs[pos[1]][pos[0]] += 1;
                            }
                        }
                    }
                    if (attackLocs[pos[1]][pos[0]] > goodLocs[0][2]) {
                        /* clear */ (goodLocs.length = 0);
                        /* add */ (goodLocs.push([pos[0], pos[1], attackLocs[pos[1]][pos[0]]]) > 0);
                    }
                    else if (attackLocs[pos[1]][pos[0]] === goodLocs[0][2]) {
                        /* add */ (goodLocs.push([pos[0], pos[1], attackLocs[pos[1]][pos[0]]]) > 0);
                    }
                }
            }
            if (goodLocs.length === 1) {
                return this.attack(/* get */ goodLocs[0][0] - 4, /* get */ goodLocs[0][1] - 4);
            }
            var lowestID = 4097;
            var finalBestLoc = [-1, -1];
            for (var index139 = 0; index139 < goodLocs.length; index139++) {
                var loc = goodLocs[index139];
                {
                    for (var dx = -1; dx <= 1; dx++) {
                        for (var dy = -1; dy <= 1; dy++) {
                            var newX = this.me.x + loc[0] - 4 + dx;
                            var newY = this.me.y + loc[1] - 4 + dy;
                            if (newX < 0 || newX >= this.fullMap.length || newY < 0 || newY >= this.fullMap.length) {
                                continue;
                            }
                            var ID = this.robotMap[newY][newX];
                            if (ID > 0 && ID < lowestID && this.getRobot(ID).team !== this.me.team) {
                                lowestID = ID;
                                finalBestLoc = [loc[0], loc[1]];
                            }
                        }
                        ;
                    }
                    ;
                }
            }
            if (finalBestLoc[0] === -1) {
                this.log("aha!");
                return null;
            }
            return this.attack(finalBestLoc[0] - this.me.x, finalBestLoc[1] - this.me.y);
        };
        /*private*/ MyRobot.prototype.updateTargetCastle = function () {
            var castleKilled = true;
            var newX;
            var newY;
            for (var dx = -2; dx <= 2; dx++) {
                for (var dy = -2; dy <= 2; dy++) {
                    newX = this.enemyCastleLocs[this.targetCastle][0] + dx;
                    newY = this.enemyCastleLocs[this.targetCastle][1] + dy;
                    if (!(newX < 0 || newX >= this.fullMap.length || newY < 0 || newY >= this.fullMap.length)) {
                        var ID = this.robotMap[newY][newX];
                        if (ID === -1 || (ID > 0 && this.getRobot(ID).team !== this.me.team)) {
                            castleKilled = false;
                        }
                    }
                }
                ;
            }
            ;
            if (castleKilled) {
                this.enemyCastleLocs[this.targetCastle] = [-1, -1];
                if (this.numCastles === 2) {
                    this.targetCastle = 1 - this.targetCastle;
                }
                else if (this.enemyCastleLocs[1][0] === -1) {
                    this.targetCastle = this.enemyCastleLocs[0][0] !== -1 ? 0 : 2;
                }
                else {
                    this.targetCastle = 1;
                }
            }
        };
        /*private*/ MyRobot.prototype.bfs = function (goalX, goalY) {
            this.locInPath = 0;
            var occupied = false;
            if (this.robotMap[goalY][goalX] > 0) {
                occupied = true;
            }
            var fuelCost = this.SPECS.UNITS[this.me.unit].FUEL_PER_MOVE;
            var maxRadius = (Math.sqrt(this.SPECS.UNITS[this.me.unit].SPEED) | 0);
            var spots = ([]);
            var spot = [this.me.x, this.me.y];
            var from = (function (s) { var a = []; while (s-- > 0)
                a.push(0); return a; })(this.fullMap.length * this.fullMap.length);
            for (var i = 0; i < from.length; i++) {
                from[i] = -1;
            }
            ;
            var closestSpot = null;
            var closestDistance = (goalX - this.me.x) * (goalX - this.me.x) + (goalY - this.me.y) * (goalY - this.me.y);
            while ((!(spot[0] === goalX && spot[1] === goalY))) {
                var left = Math.max(0, spot[0] - maxRadius);
                var top_1 = Math.max(0, spot[1] - maxRadius);
                var right = Math.min(this.fullMap[0].length - 1, spot[0] + maxRadius);
                var bottom = Math.min(this.fullMap.length - 1, spot[1] + maxRadius);
                for (var x = left; x <= right; x++) {
                    var dx = x - spot[0];
                    for (var y = top_1; y <= bottom; y++) {
                        var dy = y - spot[1];
                        if (dx * dx + dy * dy <= maxRadius * maxRadius && this.fullMap[y][x] > this.IMPASSABLE && (this.robotMap[y][x] <= 0)) {
                            if (from[y * this.fullMap.length + x] !== -1) {
                                continue;
                            }
                            var newSpot = [x, y];
                            from[y * this.fullMap.length + x] = spot[1] * this.fullMap.length + spot[0];
                            if (occupied) {
                                if ((goalX - x) * (goalX - x) + (goalY - y) * (goalY - y) < closestDistance) {
                                    closestDistance = (goalX - x) * (goalX - x) + (goalY - y) * (goalY - y);
                                    closestSpot = newSpot;
                                    continue;
                                }
                            }
                            /* add */ (spots.push(newSpot) > 0);
                        }
                    }
                    ;
                }
                ;
                if (occupied && closestSpot != null) {
                    spot = closestSpot;
                    break;
                }
                spot = (function (a) { return a.length == 0 ? null : a.shift(); })(spots);
                if (spot == null) {
                    return null;
                }
            }
            ;
            var ans = ([]);
            while ((from[spot[1] * this.fullMap.length + spot[0]] !== -1)) {
                /* add */ ans.splice(0, 0, spot);
                var prevSpot = from[spot[1] * this.fullMap.length + spot[0]];
                spot = [prevSpot % this.fullMap.length, (((prevSpot / this.fullMap.length | 0)) | 0)];
            }
            ;
            return ans;
        };
        /*private*/ MyRobot.prototype.spaceIsCootiesFree = function (x, y) {
            var robomap = this.robotMap;
            for (var index140 = 0; index140 < this.adjacentSpaces.length; index140++) {
                var adj = this.adjacentSpaces[index140];
                {
                    if (y + adj[1] > -1 && y + adj[1] < robomap.length && x + adj[0] > -1 && x + adj[0] < robomap.length) {
                        if (robomap[y + adj[1]][x + adj[0]] > 0) {
                            return false;
                        }
                    }
                }
            }
            return true;
        };
        /*private*/ MyRobot.prototype.bfsCooties = function (goalX, goalY) {
            this.locInPath = 0;
            var occupied = false;
            if (this.robotMap[goalY][goalX] > 0) {
                occupied = true;
            }
            var maxRadius = (Math.sqrt(this.SPECS.UNITS[this.me.unit].SPEED) | 0);
            var spots = ([]);
            var spot = [this.me.x, this.me.y];
            var from = (function (s) { var a = []; while (s-- > 0)
                a.push(0); return a; })(this.fullMap.length * this.fullMap[0].length);
            for (var i = 0; i < from.length; i++) {
                from[i] = -1;
            }
            ;
            var closestSpot = null;
            var closestDistance = (goalX - this.me.x) * (goalX - this.me.x) + (goalY - this.me.y) * (goalY - this.me.y);
            while ((!(spot[0] === goalX && spot[1] === goalY))) {
                var left = Math.max(0, spot[0] - maxRadius);
                var top_2 = Math.max(0, spot[1] - maxRadius);
                var right = Math.min(this.fullMap[0].length - 1, spot[0] + maxRadius);
                var bottom = Math.min(this.fullMap.length - 1, spot[1] + maxRadius);
                for (var x = left; x <= right; x++) {
                    var dx = x - spot[0];
                    for (var y = top_2; y <= bottom; y++) {
                        var dy = y - spot[1];
                        if (dx * dx + dy * dy <= maxRadius * maxRadius && this.fullMap[y][x] > this.IMPASSABLE && this.robotMap[y][x] <= 0 && this.spaceIsCootiesFree(x, y)) {
                            if (from[y * this.fullMap.length + x] !== -1) {
                                continue;
                            }
                            var newSpot = [x, y];
                            from[y * this.fullMap.length + x] = spot[1] * this.fullMap.length + spot[0];
                            if (occupied) {
                                if ((goalX - x) * (goalX - x) + (goalY - y) * (goalY - y) < closestDistance) {
                                    closestDistance = (goalX - x) * (goalX - x) + (goalY - y) * (goalY - y);
                                    closestSpot = newSpot;
                                    continue;
                                }
                            }
                            /* add */ (spots.push(newSpot) > 0);
                        }
                    }
                    ;
                }
                ;
                if (occupied && closestSpot != null) {
                    spot = closestSpot;
                    break;
                }
                spot = (function (a) { return a.length == 0 ? null : a.shift(); })(spots);
                if (spot == null) {
                    return null;
                }
            }
            ;
            var ans = ([]);
            while ((from[spot[1] * this.fullMap.length + spot[0]] !== -1)) {
                /* add */ ans.splice(0, 0, spot);
                var prevSpot = from[spot[1] * this.fullMap.length + spot[0]];
                spot = [prevSpot % this.fullMap.length, (((prevSpot / this.fullMap.length | 0)) | 0)];
            }
            ;
            return ans;
        };
        /*private*/ MyRobot.prototype.availAdjSq = function (target) {
            var i;
            if (target[0] === 0) {
                i = target[1] * -2 + 2;
            }
            else if (target[0] === -1) {
                i = target[1] * -1 + 2;
            }
            else if (target[0] === 1) {
                i = target[1] + 6;
            }
            else {
                this.log("That is not a valid target for availAdjSq(). Returning null.");
                return null;
            }
            var newX = this.me.x + this.adjacentSpaces[i][0];
            var newY = this.me.y + this.adjacentSpaces[i][1];
            var delta = 1;
            var sign = 1;
            while ((newX < 0 || newX >= this.fullMap.length || newY < 0 || newY >= this.fullMap.length || this.fullMap[newY][newX] === -1 || this.robotMap[newY][newX] > 0)) {
                if (delta >= 8) {
                    this.log("No adjacent movable spaces (from availAdjSq()).");
                    return null;
                }
                i += delta * sign;
                i %= 8;
                newX = this.me.x + this.adjacentSpaces[i][0];
                newY = this.me.y + this.adjacentSpaces[i][1];
                delta += 1;
                sign *= -1;
            }
            ;
            return this.adjacentSpaces[i];
        };
        /*private*/ MyRobot.prototype.randomAdjSq = function () {
            var rand;
            var newX;
            var newY;
            rand = ((Math.random() * 8) | 0);
            var i = 0;
            do {
                rand += 1;
                rand %= 8;
                i++;
                newX = this.me.x + this.adjacentSpaces[rand][0];
                newY = this.me.y + this.adjacentSpaces[rand][1];
                if (i >= 8) {
                    return null;
                }
            } while ((newX < 0 || newX >= this.fullMap.length || newY < 0 || newY >= this.fullMap.length || this.fullMap[newY][newX] === -1 || this.robotMap[newY][newX] > 0));
            return this.adjacentSpaces[rand];
        };
        /*private*/ MyRobot.prototype.randomOddAdjSq = function () {
            var rand;
            var newX;
            var newY;
            var pos = 1 - (this.me.x + this.me.y) % 2;
            rand = (((Math.random() * 4) | 0)) * 2 + 1 + pos;
            var i = 0;
            do {
                i++;
                if (i > 4) {
                    return null;
                }
                rand += 2;
                rand %= 8;
                newX = this.me.x + this.adjacentSpaces[rand][0];
                newY = this.me.y + this.adjacentSpaces[rand][1];
            } while ((newX < 0 || newX >= this.fullMap.length || newY < 0 || newY >= this.fullMap.length || this.fullMap[newY][newX] === -1 || this.robotMap[newY][newX] > 0));
            return this.adjacentSpaces[rand];
        };
        /*private*/ MyRobot.prototype.getCastleDir = function () {
            if (this.castleLocs[0][0] - this.me.x === 0) {
                this.castleDir = this.castleLocs[0][1] - this.me.y * -2 + 2;
            }
            else if (this.castleLocs[0][0] - this.me.x === -1) {
                this.castleDir = this.castleLocs[0][1] - this.me.y * -1 + 2;
            }
            else if (this.castleLocs[0][0] - this.me.x === 1) {
                this.castleDir = this.castleLocs[0][1] - this.me.y + 6;
            }
        };
        /*private*/ MyRobot.prototype.exploreLattice = function () {
            var fpoo;
            var newX;
            var newY;
            if (this.castleDir % 2 === 0) {
                var chooseDir = ((Math.random() * 2) | 0);
                fpoo = (chooseDir === 0) ? (this.adjacentSpaces[(this.castleDir + 4) % 8]) : this.adjacentSpaces[this.sideDir];
                fpoo = [fpoo[0] * 2, fpoo[1] * 2];
                newX = this.me.x + fpoo[0];
                newY = this.me.y + fpoo[1];
                if (newX >= 0 && newX < this.fullMap.length && newY >= 0 && newY < this.fullMap.length && this.fullMap[newY][newX] === 0 && this.robotMap[newY][newX] <= 0) {
                    return fpoo;
                }
                fpoo = (chooseDir !== 0) ? (this.adjacentSpaces[(this.castleDir + 4) % 8]) : this.adjacentSpaces[this.sideDir];
                fpoo = [fpoo[0] * 2, fpoo[1] * 2];
                newX = this.me.x + fpoo[0];
                newY = this.me.y + fpoo[1];
                if (newX >= 0 && newX < this.fullMap.length && newY >= 0 && newY < this.fullMap.length && this.fullMap[newY][newX] === 0 && this.robotMap[newY][newX] <= 0) {
                    return fpoo;
                }
            }
            else {
                var chooseDir = (((Math.random() * 2) | 0)) * 2 - 1;
                fpoo = this.adjacentSpaces[(this.castleDir + 4 + chooseDir) % 8];
                fpoo = [fpoo[0] * 2, fpoo[1] * 2];
                newX = this.me.x + fpoo[0];
                newY = this.me.y + fpoo[1];
                if (newX >= 0 && newX < this.fullMap.length && newY >= 0 && newY < this.fullMap.length && this.fullMap[newY][newX] === 0 && this.robotMap[newY][newX] <= 0) {
                    return fpoo;
                }
                fpoo = this.adjacentSpaces[(this.castleDir + 4 - chooseDir) % 8];
                fpoo = [fpoo[0] * 2, fpoo[1] * 2];
                newX = this.me.x + fpoo[0];
                newY = this.me.y + fpoo[1];
                if (newX >= 0 && newX < this.fullMap.length && newY >= 0 && newY < this.fullMap.length && this.fullMap[newY][newX] === 0 && this.robotMap[newY][newX] <= 0) {
                    return fpoo;
                }
            }
            return null;
        };
        /*private*/ MyRobot.prototype.isNextToHome = function (newX, newY) {
            if (Math.abs(newX - this.castleLocs[0][0]) <= 1 && Math.abs(newY - this.castleLocs[0][1]) <= 1) {
                return true;
            }
            return false;
        };
        /*private*/ MyRobot.prototype.goToLattice = function () {
            var newX;
            var newY;
            var rRange;
            for (var range = 1; range < 64; range++) {
                rRange = ((Math.floor(Math.sqrt(range))) | 0);
                for (var dx = -rRange; dx <= rRange; dx++) {
                    for (var dy = -rRange; dy <= rRange; dy++) {
                        if (dx * dx + dy * dy === range) {
                            newX = this.me.x + dx;
                            newY = this.me.y + dy;
                            if (this.isOnMap(newX, newY) && this.fullMap[newY][newX] === 0 && this.robotMap[newY][newX] <= 0 && (newX + newY) % 2 === 0 && !this.isNextToHome(newX, newY)) {
                                return this.bfs(newX, newY);
                            }
                        }
                    }
                    ;
                }
                ;
            }
            ;
            return null;
        };
        /*private*/ MyRobot.prototype.moveAway = function () {
            for (var dx = -2; dx <= 2; dx++) {
                for (var dy = -2; dy <= 2; dy++) {
                    if ((Math.abs(dx) === 2 && Math.abs(dy) === 2) || (dx === 0 && dy === 0)) {
                        continue;
                    }
                    var newX = this.me.x + dx;
                    var newY = this.me.y + dy;
                    if (!(newX < 0 || newX >= this.fullMap.length || newY < 0 || newY >= this.fullMap.length)) {
                        var ID = this.robotMap[newY][newX];
                        if (ID > 0 && this.getRobot(ID).team === this.me.team && (this.getRobot(ID).unit === 4 || this.getRobot(ID).unit === 0 || this.getRobot(ID).unit === 1)) {
                            return true;
                        }
                    }
                }
                ;
            }
            ;
            return false;
        };
        /*private*/ MyRobot.prototype.isOnMap = function (x, y) {
            return (x >= 0 && x < this.fullMap.length && y >= 0 && y < this.fullMap.length);
        };
        /*private*/ MyRobot.prototype.getNewUnit = function (talk) {
            {
                var array142 = this.getVisibleRobots();
                for (var index141 = 0; index141 < array142.length; index141++) {
                    var rob = array142[index141];
                    {
                        if (rob.unit === talk) {
                            var n = true;
                            {
                                var array144 = this.robs[talk];
                                for (var index143 = 0; index143 < array144.length; index143++) {
                                    var oldRobID = array144[index143];
                                    {
                                        if (rob.id === oldRobID) {
                                            n = false;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (n) {
                                /* add */ (this.robs[talk].push(rob.id) > 0);
                                break;
                            }
                        }
                    }
                }
            }
        };
        /*private*/ MyRobot.prototype.getCastleLocs = function () {
            if (this.globalTurn === 849) {
                var talk = this.getCastObj(0).signal ^ this.xorKey;
                if (talk >= 4096) {
                    this.numCastles = 1;
                    this.getEnemyCastleLocs();
                    this.getTargetCastle();
                }
                else {
                    this.numCastles = 2;
                    this.castleLocs[1][0] = talk % 64;
                    this.castleLocs[1][1] = (Math.floor((talk / 64 | 0)) | 0);
                }
            }
            else if (this.globalTurn === 850 && this.numCastles === 2) {
                var talk = this.getCastObj(0).signal ^ this.xorKey;
                if (talk < 4096) {
                    this.numCastles = 3;
                    this.castleLocs[2][0] = talk % 64;
                    this.castleLocs[2][1] = (Math.floor((talk / 64 | 0)) | 0);
                }
                this.getEnemyCastleLocs();
                this.getTargetCastle();
            }
        };
        /*private*/ MyRobot.prototype.getCastObj = function (num) {
            var visb = this.getVisibleRobots();
            for (var index145 = 0; index145 < visb.length; index145++) {
                var cast = visb[index145];
                {
                    if (cast.id === this.robs[0][num]) {
                        return cast;
                    }
                }
            }
            return null;
        };
        return MyRobot;
    }(bc19.BCAbstractRobot));
    bc19.MyRobot = MyRobot;
    MyRobot["__class"] = "bc19.MyRobot";
})(bc19 || (bc19 = {}));
//# sourceMappingURL=bundle.js.map
var specs = {"COMMUNICATION_BITS":16,"CASTLE_TALK_BITS":8,"MAX_ROUNDS":1000,"TRICKLE_FUEL":25,"INITIAL_KARBONITE":100,"INITIAL_FUEL":500,"MINE_FUEL_COST":1,"KARBONITE_YIELD":2,"FUEL_YIELD":10,"MAX_TRADE":1024,"MAX_BOARD_SIZE":64,"MAX_ID":4096,"CASTLE":0,"CHURCH":1,"PILGRIM":2,"CRUSADER":3,"PROPHET":4,"PREACHER":5,"RED":0,"BLUE":1,"CHESS_INITIAL":100,"CHESS_EXTRA":20,"TURN_MAX_TIME":200,"MAX_MEMORY":50000000,"UNITS":[{"CONSTRUCTION_KARBONITE":null,"CONSTRUCTION_FUEL":null,"KARBONITE_CAPACITY":null,"FUEL_CAPACITY":null,"SPEED":0,"FUEL_PER_MOVE":null,"STARTING_HP":200,"VISION_RADIUS":100,"ATTACK_DAMAGE":10,"ATTACK_RADIUS":[1,64],"ATTACK_FUEL_COST":10,"DAMAGE_SPREAD":0},{"CONSTRUCTION_KARBONITE":50,"CONSTRUCTION_FUEL":200,"KARBONITE_CAPACITY":null,"FUEL_CAPACITY":null,"SPEED":0,"FUEL_PER_MOVE":null,"STARTING_HP":100,"VISION_RADIUS":100,"ATTACK_DAMAGE":0,"ATTACK_RADIUS":0,"ATTACK_FUEL_COST":0,"DAMAGE_SPREAD":0},{"CONSTRUCTION_KARBONITE":10,"CONSTRUCTION_FUEL":50,"KARBONITE_CAPACITY":20,"FUEL_CAPACITY":100,"SPEED":4,"FUEL_PER_MOVE":1,"STARTING_HP":10,"VISION_RADIUS":100,"ATTACK_DAMAGE":null,"ATTACK_RADIUS":null,"ATTACK_FUEL_COST":null,"DAMAGE_SPREAD":null},{"CONSTRUCTION_KARBONITE":15,"CONSTRUCTION_FUEL":50,"KARBONITE_CAPACITY":20,"FUEL_CAPACITY":100,"SPEED":9,"FUEL_PER_MOVE":1,"STARTING_HP":40,"VISION_RADIUS":49,"ATTACK_DAMAGE":10,"ATTACK_RADIUS":[1,16],"ATTACK_FUEL_COST":10,"DAMAGE_SPREAD":0},{"CONSTRUCTION_KARBONITE":25,"CONSTRUCTION_FUEL":50,"KARBONITE_CAPACITY":20,"FUEL_CAPACITY":100,"SPEED":4,"FUEL_PER_MOVE":2,"STARTING_HP":20,"VISION_RADIUS":64,"ATTACK_DAMAGE":10,"ATTACK_RADIUS":[16,64],"ATTACK_FUEL_COST":25,"DAMAGE_SPREAD":0},{"CONSTRUCTION_KARBONITE":30,"CONSTRUCTION_FUEL":50,"KARBONITE_CAPACITY":20,"FUEL_CAPACITY":100,"SPEED":4,"FUEL_PER_MOVE":3,"STARTING_HP":60,"VISION_RADIUS":16,"ATTACK_DAMAGE":20,"ATTACK_RADIUS":[1,16],"ATTACK_FUEL_COST":15,"DAMAGE_SPREAD":3}]};
var robot = new bc19.MyRobot(); robot.setSpecs(specs);