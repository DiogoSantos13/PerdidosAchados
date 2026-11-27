# Perdidos e Achados

Trabalho prático de Desenvolvimento de Aplicações Móveis (2026/27) — Licenciatura em Engenharia Informática.

Aplicação Android nativa (Kotlin) para publicar e procurar objetos perdidos e achados no campus.

## Estrutura

| Pasta | Conteúdo |
|---|---|
| `android/` | Projeto Android Studio (Kotlin, Views + View Binding) |
| `api/` | API REST em Python (Flask + SQLite), com dados de exemplo |

## Funcionalidades

- **Câmara** — fotografar o objeto ao publicá-lo (ou escolher da galeria).
- **GPS** — registar onde o objeto foi perdido/achado, ordenar a lista por proximidade ("Perto de mim"), ver a distância e abrir o local no mapa.
- **API REST** — listar, pesquisar, criar, editar e eliminar objetos; registo e login.
- **Dados locais** — sessão em Shared Preferences; última lista de objetos em Internal Storage (consulta sem Internet); fotografias em Cache (gerida pelo Glide).
- **Autenticação e controlo de acesso** — consulta livre; publicar e ver contactos exige login; editar/eliminar só o dono ou o administrador.
- **Validação** — todos os campos na inserção e na edição; confirmação antes de eliminar, de terminar sessão e de sair sem guardar.
- **Sobre** — separador no ecrã inicial com curso, disciplina, ano letivo, autores e bibliotecas.

## Correr a API

```bash
cd api
pip install -r requirements.txt
python app.py          # fica em http://localhost:5000
```

A base de dados (`perdidos_achados.db`) é criada no primeiro arranque, já com dados de exemplo.
Para repor os dados iniciais, apagar esse ficheiro e a pasta `fotos/`.

Para alojar num servidor WSGI, o ponto de entrada é `from app import app as application`.

### Utilizadores iniciais

| Papel | E-mail | Palavra-passe |
|---|---|---|
| Administrador | admin@ipt.pt | Admin123! |
| Utilizador | aluno25001@ipt.pt | Marta123! |
| Utilizador | aluno25002@ipt.pt | Tiago123! |
| Utilizador | aluno24117@ipt.pt | Ines123! |

### Endpoints

| Método | Caminho | Descrição | Acesso |
|---|---|---|---|
| POST | `/api/auth/registar` | Criar conta | livre |
| POST | `/api/auth/login` | Iniciar sessão (devolve token) | livre |
| GET | `/api/objetos?tipo=&q=&estado=` | Listar/pesquisar objetos | livre |
| GET | `/api/objetos/meus` | Objetos do utilizador | login |
| GET | `/api/objetos/{id}` | Um objeto | livre |
| POST | `/api/objetos` | Publicar objeto | login |
| PUT | `/api/objetos/{id}` | Alterar objeto | dono ou admin |
| DELETE | `/api/objetos/{id}` | Eliminar objeto | dono ou admin |
| GET | `/api/fotos/{ficheiro}` | Fotografia | livre |

Autenticação: cabeçalho `Authorization: Bearer <token>`.

## Correr a app

1. Abrir a pasta `android/` no Android Studio e esperar pela sincronização do Gradle.
2. Com a API a correr no mesmo computador, executar no emulador (a app usa `http://10.0.2.2:5000/`).
3. Para um telemóvel real ou para a Google Play, alterar `BASE_URL` em
   `retrofit/RetrofitInitializer.kt` para o endereço público da API.

## Bibliotecas e código de terceiros

- [Retrofit 2.9.0](https://square.github.io/retrofit/) e conversor Gson — chamadas à API REST (Apache 2.0)
- [Gson](https://github.com/google/gson) — conversão JSON (Apache 2.0)
- [Glide 4.16.0](https://github.com/bumptech/glide) — descarregar e mostrar as fotografias, com cache (BSD, MIT e Apache 2.0)
- [OkHttp](https://square.github.io/okhttp/) — usado internamente pelo Retrofit (Apache 2.0)
- AndroidX e [Material Components](https://github.com/material-components/material-components-android) (Apache 2.0)
- Ícones [Material Symbols](https://fonts.google.com/icons) (Apache 2.0)
- [Flask](https://flask.palletsprojects.com/) — API (BSD)
- Exemplos das aulas de DAM (API, Fragmentos, Sensores, Armazenamento) — base da estrutura do código
