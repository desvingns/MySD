package dev.mysd.android.product

internal fun testShell(
    destination: ProductDestinationUi? = ProductDestinationUi.CAMPAIGN,
    showNavigation: Boolean = true,
) = ProductShellUi(
    energy = 9,
    energyCap = 12,
    credits = 640,
    crystals = 4,
    rewardTrackExperience = 46,
    rewardTrackTarget = 50,
    selectedDestination = destination,
    showMetaNavigation = showNavigation,
)

internal fun testApp(
    surface: ProductSurfaceUi,
    overlay: ProductOverlayUi? = null,
) = ProductUiModel(
    shell = testShell(
        destination = when (surface) {
            is ProductSurfaceUi.Roster -> ProductDestinationUi.ROSTER
            is ProductSurfaceUi.Tech -> ProductDestinationUi.TECH
            is ProductSurfaceUi.Shop -> ProductDestinationUi.SHOP
            is ProductSurfaceUi.Arena -> ProductDestinationUi.ARENA
            is ProductSurfaceUi.Launch -> null
            else -> ProductDestinationUi.CAMPAIGN
        },
        showNavigation = surface !is ProductSurfaceUi.Launch &&
            surface !is ProductSurfaceUi.Battle,
    ),
    surface = surface,
    overlay = overlay,
)

internal fun testBattleScene(
    phase: BattlePhaseUi = BattlePhaseUi.ACTIVE,
    result: BattleResultUi? = null,
) = BattleSceneUi(
    stageTitle = "Тропа углей",
    phase = phase,
    wave = 3,
    totalWaves = 10,
    resource = 120,
    resourceCap = 220,
    speedMultiplier = 1,
    baseHealth = 84,
    baseMaxHealth = 100,
    slots = listOf(
        BattleSlotUi(
            id = "slot-1",
            index = 0,
            xFraction = 0.36f,
            yFraction = 0.42f,
            towerName = null,
            level = 0,
            cost = 0,
            canAfford = true,
            maxLevel = false,
        ),
    ),
    enemies = listOf(
        BattleEntityUi(
            id = "enemy-1",
            label = "Пепельный бегун",
            xFraction = 0.54f,
            yFraction = 0.58f,
            health = 12,
            maxHealth = 18,
            role = BattleEntityRoleUi.ENEMY_LIGHT,
        ),
    ),
    allies = emptyList(),
    abilities = listOf(
        HeroAbilityUi(
            id = "hero-solar-pulse",
            title = "Солнечный импульс",
            ready = true,
            cooldownSeconds = 0,
        ),
    ),
    availableTowerIds = listOf(
        "tower-ember-needle",
        "tower-sunburst-mortar",
        "tower-glass-snare",
        "tower-seed-forge",
    ),
    availableAllyIds = listOf(
        "ally-cinder-guard",
        "ally-bright-skirmisher",
        "ally-arc-striker",
    ),
    towerBuildCosts = mapOf(
        "tower-ember-needle" to 35,
        "tower-sunburst-mortar" to 55,
        "tower-glass-snare" to 45,
        "tower-seed-forge" to 50,
    ),
    allyDeployCosts = mapOf(
        "ally-cinder-guard" to 34,
        "ally-bright-skirmisher" to 28,
        "ally-arc-striker" to 38,
    ),
    enhancementChoices = listOf(
        EnhancementChoiceUi("enhancement-keen-sparks", "Острые искры", "Точность", 1),
        EnhancementChoiceUi("enhancement-quick-coils", "Быстрые катушки", "Темп", 2),
        EnhancementChoiceUi("enhancement-long-sight", "Дальний прицел", "Дальность", 3),
    ),
    enhancementRerollsRemaining = 1,
    result = result,
)
