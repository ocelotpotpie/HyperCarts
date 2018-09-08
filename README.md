HyperCarts
==========
A Bukkit plugin to control the maximum speed of minecarts.


Commands
--------
User commands:

 * `/cart-speed help` - Show usage help.
 * `/cart-speed` - Show your current personal maximum minecart speed.
 * `/cart-speed <number>` - Set a new personal maximum minecart speed.

Administrative commands:

 * `/hypercarts help` - Show usage help.
 * `/hypercarts max` - Show the server-wide maximum minecart speed.
 * `/hypercarts max <number>` - Set the server-wide maximum minecart speed.
 * `/hypercarts reload` - Reload the configuration.


Configuration
-------------
| Setting           | Description |
| :---              | :--- |
| `debug.log-configuration` | If true, log the configuration on load. |
| `debug.to-log`    | If true, debug messages also go to the server log. |
| `debug.level`     | If non-zero, send debug messages to players with the `hypercarts.debug` permission. Higher numbers send more information. |
| `max-speed`       | The maximum speed value of a minecart, set when the vehicle is created. The vanilla default is 0.4. |
| `slow-down-ticks` | The time period in ticks that carts (default 40) will be slowed to vanilla speed after encountering a curve or ramp. |

 * Additionally, each player has his preferred cart speed stored in `players.yml`.

Permissions
-----------

 * `hypercarts.admin` - Permission to use the `/hypercarts` command.
 * `hypercarts.user` - Permission to use the `/cart-speed` command.
 * `hypercarts.debug` - Permission to see debug messages.
