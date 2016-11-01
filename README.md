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
 * `/hypercarts - Show the server-wide maximum minecart speed.
 * `/hypercarts <number>§f - Set the server-wide maximum minecart speed.


Configuration
-------------
| Setting | Description |
| :--- | :--- |
| `max-speed` | The maximum speed value of a minecart, set when the vehicle is created. The vanilla default is 0.4. |

 * Additionally, each player has his preferred cart speed stored in `players.yml`.

Permissions
-----------

 * `hypercarts.admin` - Permission to use the `/hypercarts` command.
 * `hypercarts.user` - Permission to use the `/cart-speed` command.
