# Настройка подписи release-APK (обновление «поверх»)

Чтобы приложение можно было ставить новой версией **поверх** старой (без «конфликта
пакетов» и без потери данных), все сборки должны быть подписаны **одним постоянным
ключом**. Раньше CI собирал debug-APK со случайным ключом на каждом раннере — отсюда
и ошибка несовместимости. Теперь CI собирает подписанный release-APK ключом из
GitHub Secrets. Ниже — что нужно сделать **один раз**.

> ВАЖНО: keystore и пароли НЕЛЬЗЯ коммитить в репозиторий. Только в GitHub Secrets.
> И обязательно сохрани keystore себе в надёжное место — если потеряешь, будущие
> версии этим же ключом уже не подпишешь, и обновление «поверх» снова сломается.

## 1. Сгенерировать keystore (локально, один раз)

Нужен установленный JDK (у тебя есть — `keytool` идёт с ним). В терминале:

```bash
keytool -genkeypair -v \
  -keystore workout-studio-release.keystore \
  -alias workout-studio \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass ВАШ_ПАРОЛЬ_ХРАНИЛИЩА \
  -keypass ВАШ_ПАРОЛЬ_КЛЮЧА \
  -dname "CN=Workout Studio, O=nikita, C=RU"
```

- `alias` = `workout-studio` (запомни — пойдёт в секрет `KEY_ALIAS`).
- Пароли можешь сделать одинаковыми для простоты.
- Файл `workout-studio-release.keystore` появится в текущей папке. Сохрани его!

## 2. Превратить keystore в base64 (для секрета)

GitHub Secrets хранит текст, а keystore бинарный — кодируем в base64:

```bash
# Linux / macOS / Git Bash:
base64 -w0 workout-studio-release.keystore > keystore.base64.txt

# Windows PowerShell (если base64 нет под рукой):
[Convert]::ToBase64String([IO.File]::ReadAllBytes("workout-studio-release.keystore")) > keystore.base64.txt
```

Открой `keystore.base64.txt` — это одна длинная строка, её и вставишь в секрет.

## 3. Добавить секреты в GitHub

Репозиторий → **Settings → Secrets and variables → Actions → New repository secret**.
Создай четыре секрета:

| Имя секрета         | Значение                                             |
|---------------------|------------------------------------------------------|
| `KEYSTORE_BASE64`   | содержимое `keystore.base64.txt` (вся строка)        |
| `KEYSTORE_PASSWORD` | пароль хранилища (`-storepass` из шага 1)             |
| `KEY_ALIAS`         | `workout-studio`                                     |
| `KEY_PASSWORD`      | пароль ключа (`-keypass` из шага 1)                  |

## 4. Запустить сборку

Пушни в `main` (или запусти workflow вручную через Actions → Build APK → Run
workflow). В артефактах появится **`workout-studio-release-apk`** — это подписанный
`app-release.apk`.

## 5. Первая установка

- На телефоне **один раз удали** текущую версию (у неё старая debug-подпись — иначе
  новая release-подпись не встанет поверх). Данные там всё равно можно удалить, ты
  подтвердил.
- Установи новый `app-release.apk`.
- **Дальше** все следующие release-сборки будут ставиться **поверх** без удаления —
  упражнения, отчёты и настройки сохранятся.

## Как это устроено в коде

- `app/build.gradle.kts` — `signingConfigs.release` читает keystore/пароли из
  переменных окружения (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
  `KEY_PASSWORD`). Если их нет (локальная сборка без ключа) — конфиг пропускается,
  сборка не падает.
- `.github/workflows/build.yml` — декодирует `KEYSTORE_BASE64` во временный файл,
  прокидывает пароли и собирает `assembleRelease`. Если секрета нет (форк) —
  откатывается на debug-сборку, чтобы CI не падал.
