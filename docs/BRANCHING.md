# Ветвление приложений (app-branch scheme)

Каждое приложение UVOS живёт в **своей ветке** `app/<имя>`. `main` —
интеграционная ветка: docs/, scripts/, tools/, uvos-dashboard/ и
сборки-интегратор совместимых APK.

## Схема

| Ветка | Приложение | Worktree (рабочий каталог) |
|---|---|---|
| `app/files` | uvos-files | — (пока из main) |
| `app/gallery` | uvos-gallery | — (пока из main) |
| `app/music` | uvos-music | — (пока из main) |
| `app/settings` | uvos-settings | — (пока из main) |
| `app/terminal` | uvos-terminal | — (пока из main) |
| `app/security` | uvos-security | `../UVOC-wt/security` |
| `app/clock` | uvos-clock | `../UVOC-wt/clock` |
| `app/calculator` | uvos-calculator | `../UVOC-wt/calculator` |
| `app/compass` | uvos-compass | `../UVOC-wt/compass` |

Все ветки ответвлены от `main` в точке `afe599b`.

## Правила

- Приложение разрабатывается **только в своей ветке** (через worktree,
  чтобы не переключать checkout `main` — там работают параллельные
  процессы).
- В `main` новые приложения **не коммитятся напрямую**: партия
  попадает в main только через явный merge-коммит интегратора после
  успешной сборки.
- Сборка приложения: в worktree ветки приложение — самостоятельный
  Gradle-проект (`apps/uvos-<имя>`), сборка последовательно, одна
  за раз: `set JAVA_HOME=D:\Java\jdk-21 && gradlew.bat :app:assembleDebug`.
- Переключаться внутри worktree нельзя — каждому checkout-_branch
  соответствует своё дерево; для новой задачи используйте
  `git worktree add ../UVOC-wt/<имя> app/<имя>`.
