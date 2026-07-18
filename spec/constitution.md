# Конституция MySD

1. Игра поставляется только на Android; headless JVM-контур используется для симуляции и тестов.
2. Симуляция работает детерминированно на фиксированных 20 Hz; ввод идёт через команды.
3. Render/input читают immutable snapshots и не владеют authoritative state.
4. Run save и profile store версионируются с первой схемы и имеют migration tests.
5. Контент и баланс data-driven; неизвестные reference-механики не кодируются как факт.
6. MySD использует только оригинальные IP, арты, звуки, тексты, иконки и числа.
7. Raw reference evidence локально и не попадает в git.
8. Ads, IAP и Arena в первом релизе представлены интерфейсами и детерминированными local fakes.
9. Gate 1 предшествует production-требованиям; Gate 2 предшествует production gameplay code.
10. MyEngine подключается composite build по commit из `gradle/myengine.lock`.
