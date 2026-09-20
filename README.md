# Halo

Glowing outlines for players on Paper 1.21+. Every glow is written in one file, `glows.yml`: a single colour, colours
that cycle or bounce, or a flashing glow. There is no built-in menu, build one with
DeluxeMenus (or any menu plugin) using the commands and placeholders below.

## Files

| File | What it holds |
| --- | --- |
| `glows.yml` | All the glows |
| `config.yml` | Permissions on or off, worlds where nobody glows |
| `messages.yml` | Every message the plugin sends |

## Writing a glow

```yaml
glows:
  red:
    color: red

  rainbow:
    type: CYCLE
    interval: 10
    colors: [red, gold, yellow, green, aqua, blue, light_purple]

  fire:
    type: BOUNCE
    interval: 5
    colors: [dark_red, red, gold, yellow]

  police:
    type: FLASH
    interval: 4
    colors: [red, blue]
```

| Key | Meaning |
| --- | --- |
| `type` | `SINGLE`, `CYCLE`, `BOUNCE` or `FLASH`. Without it, one colour is `SINGLE` and more are `CYCLE` |
| `color` / `colors` | One of the 16 colours (`dark_red`, `gold`...) or its code (`c`, `&c`) |
| `interval` | Ticks between two changes, 20 is one second |
| `display` | The name in `/glow list` and messages |
| `permission` | Replaces `halo.glow.<id>` |

The types:

- `SINGLE`: one colour.
- `CYCLE`: the colours one after the other, over and over.
- `BOUNCE`: the colours forwards, then backwards.
- `FLASH`: every colour blinks on and off.

The outline of a glowing player can only have one of Minecraft's 16 colours, which is a limit of the game.

## Commands

| Command | Use |
| --- | --- |
| `/glow equip <id>`, `unequip`, `list` | Pick a glow |
| `/glow reload` | Reload all files |
| `/glow admin set <player> <id>` | Give a player a glow, even without the permission |
| `/glow admin clear <player>` | Remove a player's glow |

`/halo` works the same as `/glow`.

## Permissions

- `halo.glow.<id>`: use one glow (or set your own with `permission:`)
- `halo.glow.*`: use all of them (op by default)
- `halo.use`: use `/glow` (everybody by default)
- `halo.admin`: reload, admin set and clear

Set `use-permissions: false` in `config.yml` to let everybody use every glow.

## Placeholders (PlaceholderAPI)

- `%halo_id%` and `%halo_display%`: the glow the player has picked
- `%halo_active%`: `true` while a glow is showing (a flashing glow has dark moments)
- `%halo_color%`: the colour showing right now
- `%halo_equipped_<id>%` and `%halo_owned_<id>%`: `true` or `false`

## DeluxeMenus example

```yaml
items:
  sunset:
    material: ORANGE_DYE
    slot: 10
    display_name: "&6Sunset"
    view_requirement:
      requirements:
        owned:
          type: string equals
          input: "%halo_owned_sunset%"
          output: "true"
    left_click_commands:
      - "[player] glow equip sunset"
```

## Good to know

- **TAB:** nothing has to be added to TAB's files. When TAB is installed, Halo adds the glow colour to the end of the
  prefix TAB already gives the player, and TAB's name tag team carries the colour, so rank prefixes keep working and
  animated glows change as fast as you set them. The name above the head takes the glow colour too. Set
  `tab-integration: false` in `config.yml` to leave TAB alone.
- Without TAB, Halo moves a glowing player into a scoreboard team called `halo_<colour>` on the main scoreboard, and puts
  them back in their old team when the glow ends.
- The glow comes back after a relog, a respawn and a world change, and is off in the worlds listed in `disabled-worlds`.

## Building

```
./gradlew build
```

The jar is in `build/libs`. Licensed under MIT.
