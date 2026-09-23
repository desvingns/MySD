package dev.mysd.android.product

/** Original Russian presentation copy for release-one machine IDs. */
internal fun productTitle(id: String): String = when (id) {
    "pack-emberwatch-release-one" -> "Рубежи Маяка"

    "stage-ember-path" -> "Тропа углей"
    "stage-glass-garden" -> "Стеклянный сад"
    "stage-brass-ravine" -> "Латунный разлом"
    "stage-pulse-vault" -> "Импульсный свод"
    "stage-night-orchard" -> "Ночной питомник"
    "stage-dawn-engine" -> "Двигатель рассвета"

    "region-ember" -> "Угольный сектор"
    "region-orbit" -> "Орбитальный край"

    "TOWER",
    "tower",
    -> "Башенный модуль"
    "ALLY",
    "ally",
    -> "Мобильный боец"
    "HERO",
    "hero",
    -> "Навык командира"

    "tower-ember-needle" -> "Угольная игла"
    "tower-sunburst-mortar" -> "Солнечная мортира"
    "tower-glass-snare" -> "Стеклянная сеть"
    "tower-seed-forge" -> "Кузница семян"

    "ally-cinder-guard" -> "Пепельный страж"
    "ally-bright-skirmisher" -> "Светлый разведчик"
    "ally-arc-striker" -> "Дуговой стрелок"

    "enemy-ash-runner" -> "Пепельный бегун"
    "enemy-cinder-swarm" -> "Рой искр"
    "enemy-brass-shell" -> "Латунный панцирь"
    "enemy-void-disruptor" -> "Глушитель пустоты"
    "enemy-siege-bloom" -> "Осадный бутон"
    "enemy-sky-shard" -> "Небесный осколок"
    "boss-iron-orchid" -> "Железная орхидея"
    "boss-night-engine" -> "Ночной двигатель"

    "hero-hearth-repair" -> "Ремонт очага"
    "hero-solar-pulse" -> "Солнечный импульс"

    "enhancement-keen-sparks" -> "Острые искры"
    "enhancement-quick-coils" -> "Быстрые катушки"
    "enhancement-long-sight" -> "Дальний прицел"
    "enhancement-radiant-burst" -> "Лучистый залп"
    "enhancement-glass-frost" -> "Стеклянный иней"
    "enhancement-iron-bloom" -> "Железное цветение"
    "enhancement-bright-arms" -> "Светлое вооружение"
    "enhancement-swift-step" -> "Стремительный шаг"
    "enhancement-deep-reserve" -> "Глубокий резерв"
    "enhancement-wide-vault" -> "Просторный накопитель"
    "enhancement-ember-wall" -> "Угольный заслон"
    "enhancement-heroic-cycle" -> "Командный цикл"

    "tech-branch-tower" -> "Башенная матрица"
    "tech-branch-ally" -> "Отрядная связь"
    "tech-branch-economy" -> "Ресурсная сеть"
    "tech-branch-hero" -> "Контур командира"
    "tech-1-1" -> "Калибровка линз"
    "tech-1-2" -> "Точный привод"
    "tech-1-3" -> "Перегретый контур"
    "tech-2-1" -> "Полевая выучка"
    "tech-2-2" -> "Слаженный строй"
    "tech-2-3" -> "Несокрушимый отряд"
    "tech-3-1" -> "Чистый сбор"
    "tech-3-2" -> "Расширенный резерв"
    "tech-3-3" -> "Замкнутый цикл"
    "tech-4-1" -> "Быстрый отклик"
    "tech-4-2" -> "Резонанс навыка"
    "tech-4-3" -> "Командный импульс"

    "shop-supply-small" -> "Малый энергоблок"
    "shop-supply-large" -> "Большой энергоблок"
    "shop-rewarded-cache" -> "Сигнальный тайник"
    "shop-purchase-cache" -> "Экспедиционный контейнер"

    "SOFT_CURRENCY",
    "soft-currency",
    -> "Обмен игровых ресурсов"
    "REWARDED_STUB",
    "rewarded-stub",
    -> "Локальная видеозаглушка"
    "PURCHASE_STUB",
    "purchase-stub",
    -> "Локальная имитация покупки"

    else -> when {
        id.startsWith("reward-tier-") -> {
            val tier = id.removePrefix("reward-tier-").toIntOrNull()
            if (tier != null) "Контейнер маршрута $tier" else "Контейнер маршрута"
        }
        id.contains("purchase", ignoreCase = true) -> "Проверка локальной покупки"
        id.contains("rewarded", ignoreCase = true) -> "Проверка локальной видеозаглушки"
        id.startsWith("arena-formation-") || id.startsWith("formation-") -> {
            val ordinal = id.substringAfterLast('-').toIntOrNull() ?: 0
            ARENA_FORMATION_NAMES[ordinal % ARENA_FORMATION_NAMES.size]
        }
        id.startsWith("slot-") -> "Точка обороны"
        id.startsWith("wave-") -> "Волна экспедиции"
        else -> "Сигнальный модуль"
    }
}

private val ARENA_FORMATION_NAMES = listOf(
    "Зеркальный дозор",
    "Контур кометы",
    "Стражи зари",
    "Тихий резонанс",
    "Медная орбита",
    "Полярный клин",
)

internal fun productDescription(id: String): String = when (id) {
    "tower-ember-needle" -> "Скоростная башня для постоянного давления на одиночные цели."
    "tower-sunburst-mortar" -> "Тяжёлый залп поражает группу противников."
    "tower-glass-snare" -> "Контрольная башня замедляет продвижение волны."
    "tower-seed-forge" -> "Поддерживающая башня укрепляет ресурсный контур."
    "ally-cinder-guard" -> "Стойкий защитник удерживает переднюю линию."
    "ally-bright-skirmisher" -> "Манёвренный боец быстро перехватывает лёгкие цели."
    "ally-arc-striker" -> "Дальний боец атакует из безопасной позиции."
    "hero-hearth-repair" -> "Восстанавливает прочность орбитального ядра."
    "hero-solar-pulse" -> "Поражает область мощным энергетическим импульсом."
    "enhancement-keen-sparks" -> "Повышает урон башенных атак."
    "enhancement-quick-coils" -> "Ускоряет цикл перезарядки."
    "enhancement-long-sight" -> "Расширяет рабочую дистанцию обороны."
    "enhancement-radiant-burst" -> "Расширяет радиус взрывного поражения."
    "enhancement-glass-frost" -> "Усиливает замедляющий эффект."
    "enhancement-iron-bloom" -> "Увеличивает запас здоровья союзников."
    "enhancement-bright-arms" -> "Повышает силу союзного отряда."
    "enhancement-swift-step" -> "Ускоряет передвижение союзников."
    "enhancement-deep-reserve" -> "Увеличивает пассивный приток плазмы."
    "enhancement-wide-vault" -> "Расширяет вместимость ресурсного накопителя."
    "enhancement-ember-wall" -> "Снижает урон, прошедший к ядру."
    "enhancement-heroic-cycle" -> "Сокращает восстановление навыков командира."
    "shop-supply-small" -> "Небольшое пополнение энергии за игровые кредиты."
    "shop-supply-large" -> "Расширенное пополнение энергии за игровые кредиты."
    "shop-rewarded-cache" -> "Видеозаглушка без рекламы, сети и начисления награды."
    "shop-purchase-cache" -> "Демонстрационная покупка без оплаты, аккаунта и начисления ресурсов."
    "tech-branch-tower" -> "Постоянные улучшения всех оборонительных башен."
    "tech-branch-ally" -> "Постоянные улучшения мобильного отряда."
    "tech-branch-economy" -> "Постоянные улучшения ресурсной системы."
    "tech-branch-hero" -> "Постоянные улучшения навыков командира."
    else -> when {
        id.startsWith("tech-1-") -> "Развитие башенной матрицы."
        id.startsWith("tech-2-") -> "Развитие отрядной связи."
        id.startsWith("tech-3-") -> "Развитие ресурсной сети."
        id.startsWith("tech-4-") -> "Развитие контура командира."
        id.startsWith("reward-tier-") -> "Награда за продвижение по маршруту."
        else -> "Оригинальный модуль экспедиции MySD."
    }
}
