# Perdidos e Achados

Trabalho prático de Desenvolvimento de Aplicações Móveis (2026/27) — Licenciatura em Engenharia Informática.

Aplicação Android nativa (Kotlin) para publicar e procurar objetos perdidos e achados no campus.

**Autores:** Diogo Santos (n.º 27223) e Gabriel Vital (n.º 27217)
**Package:** `pt.uptomar.dam2026.perdidosachados`

## Estrutura

| Pasta | Conteúdo |
|---|---|
| `android/` | Projeto Android Studio (Kotlin, Views + View Binding) |

## Funcionalidades

- **Câmara** — fotografar o objeto ao publicá-lo (ou escolher da galeria).
- **GPS** — registar onde o objeto foi perdido/achado, ordenar a lista por proximidade ("Perto de mim"), ver a distância e abrir o local no mapa.
- **API REST** — listar, pesquisar, criar, editar e eliminar objetos; registo e login.
- **Dados locais** — sessão em Shared Preferences; última lista de objetos em Internal Storage (consulta sem Internet); fotografias em Cache.
- **Autenticação e controlo de acesso** — consulta livre; publicar e ver contactos exige login; editar/eliminar só o dono ou o administrador.
- **Validação** — todos os campos na inserção e na edição; confirmação antes de eliminar, de terminar sessão e de sair sem guardar.
- **Sobre** — separador no ecrã inicial com curso, disciplina, ano letivo, autores e bibliotecas.

## API REST

A API está num repositório próprio: **https://github.com/DiogoSantos13/PerdidosAchados-API**
(Python + Flask + SQLite). Lá estão as instruções para a correr no PC ou alojar online,
as contas iniciais e a lista de endpoints.

## Correr a app

1. Abrir a pasta `android/` no Android Studio e esperar pela sincronização do Gradle.
2. Pôr a API a correr no PC (`python app.py` no repositório da API, janela aberta).
3. Com o emulador ligado, reencaminhar a porta 5000 do emulador para o PC (repetir sempre que o emulador reinicia):
   ```
   "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" reverse tcp:5000 tcp:5000
   ```
   A app usa `http://localhost:5000/`.
4. Para um telemóvel real ou para a Google Play, alterar `BASE_URL` em
   `retrofit/RetrofitInitializer.kt` para o endereço público da API (HTTPS).
   Por segurança a app só aceita HTTP sem cifra para `10.0.2.2`/`localhost`
   (ver `res/xml/network_security_config.xml`).

Notas para Windows/OneDrive: a pasta `build` é criada em `~/.gradle-builds/PerdidosAchados`
(o OneDrive bloqueia ficheiros e fazia falhar a compilação) e `android.overridePathCheck=true`
permite compilar num caminho com acentos.

## Bibliotecas e código de terceiros

- [Retrofit 2.9.0](https://square.github.io/retrofit/) e conversor Gson — chamadas à API REST (Apache 2.0)
- [Gson](https://github.com/google/gson) — conversão JSON (Apache 2.0)
- [OkHttp](https://square.github.io/okhttp/) — usado internamente pelo Retrofit (Apache 2.0)
- AndroidX e [Material Components](https://github.com/material-components/material-components-android) (Apache 2.0)
- Ícones [Material Symbols](https://fonts.google.com/icons) (Apache 2.0)
- [Flask](https://flask.palletsprojects.com/) — API (BSD)
- Exemplos das aulas de DAM (API, Fragmentos, Sensores, Armazenamento) — base da estrutura do código
