# ZiroFit Android - Explore Tab Replication Plan

**Created:** 2026-06-02
**Source:** iOS app at `V:\Ziro-Fit\` → Android app at `V:\zirofit-android\`

---

## 1. Architecture Overview (iOS Source)

### iOS Explore Tab File Map

| Layer | Files | Lines |
|-------|-------|-------|
| **Main View** | `PersonalExploreView.swift` | 334 |
| **Components** | `ExploreComponents.swift` | 1103 |
| **Sub-Views** | `TrainerDiscoveryView.swift` (472), `TrainerMapView.swift` (1208), `EventDetailView.swift` (311), `EventsListView.swift` (119), `ExploreEmptyTrainersView.swift` (81), `ExploreEmptyEventsView.swift` (70) | |
| **ViewModels** | `ExploreViewModel.swift` (547), `TrainerDiscoveryViewModel.swift` (267), `EventsViewModel.swift` (102), `EventDetailViewModel.swift` (55) | |
| **Models** | `ExploreModels.swift` (218) | |
| **API Service** | `APIService+Explore.swift` (167) | |
| **Navigation** | `MainTabView.swift`, `CustomTabBar.swift` | |

### iOS Explore Tab Navigation Flow

```
PersonalExploreView (main screen)
├── [Sheet] CityPickerSheet
├── [Sheet] TrainerDiscoveryView (search/discovery)
│   ├── [Sheet] FilterSheet
│   ├── [Sheet] PublicTrainerProfileView
│   └── [Sheet] EventDetailView
├── [Sheet] TrainerMapView (interactive map)
│   ├── [Sheet] PublicTrainerProfileView
│   └── [Sheet] EventDetailView
├── [Sheet] EventsListView
├── [Sheet] EventDetailView
└── [Sheet] PublicTrainerProfileView
```

### iOS Explore Tab Sections (main screen)

```
┌─────────────────────────────────┐
│  [Floating Header]              │
│  City ▼  |  🔍  🗺️            │
├─────────────────────────────────┤
│  [Segmented Control]            │
│  ● Trainers  ○ Events          │
├─────────────────────────────────┤
│  TRAINERS TAB:                  │
│  ┌──────────────────────────┐   │
│  │ Featured Specialist      │   │
│  │ (Spotlight Hero Card)    │   │
│  └──────────────────────────┘   │
│  Browse by Category             │
│  [All][Yoga][HIIT][Strength]…   │
│  TRENDING SEARCHES              │
│  #Strength #Yoga #HIIT…        │
│  Trainers Near You → See All    │
│  [Card][Card][Card]… (H-scroll)│
│  Featured Trainers              │
│  [Card][Card][Card]… (H-scroll)│
│  Ziro Recommends                │
│  [Card][Card][Card]… (H-scroll)│
│  ┌──────────────────────────┐   │
│  │ Interactive Match Map    │   │
│  └──────────────────────────┘   │
│                                  │
│  EVENTS TAB:                    │
│  Featured Events                │
│  [Carousel Card][Card][Card]    │
│  Upcoming Events                │
│  Today:                         │
│  [Event Row]                    │
│  [Event Row]                    │
│  Tomorrow:                      │
│  [Event Row]                    │
└─────────────────────────────────┘
```

---

## 2. Android Current State (Gap Analysis)

### ✅ Already Implemented

| Feature | File | Status |
|---------|------|--------|
| Main Explore Screen | `ExploreScreen.kt` | Basic layout, needs major upgrade |
| Explore Header | `ExploreScreen.kt` (ExploreHeader) | Basic city + map button |
| Featured Trainers Carousel | `ExploreScreen.kt` | Exists but uses old model |
| All Trainers Section | `ExploreScreen.kt` | Exists |
| Featured Events Carousel | `ExploreScreen.kt` | Exists |
| Category Filter | `ExploreScreen.kt` | Exists |
| Upcoming Events (grouped) | `ExploreScreen.kt` | Exists |
| Explore ViewModel | `ExploreViewModel.kt` | Basic - needs significant expansion |
| Trainer Discovery Screen | `TrainerDiscoveryScreen.kt` | Good foundation |
| Trainer Discovery VM | `TrainerDiscoveryViewModel.kt` | Good, missing pagination |
| Events List Screen | `EventsListScreen.kt` | Good |
| Events ViewModel | `EventsViewModel.kt` | Good |
| Event Detail Screen | `EventDetailScreen.kt` | Needs host info, map |
| Event Detail VM | `EventDetailViewModel.kt` | Good |
| Trainer Map Screen | `TrainerMapScreen.kt` | Basic, needs events |
| Trainer Public Profile | `TrainerPublicProfileScreen.kt` | Complete |
| Trainer Events CRUD | `TrainerEventsScreen.kt` | Trainer-only feature |
| Explore Repository | `ExploreRepository.kt` | Good |
| API Endpoints | `ZiroApi.kt` | All endpoints exist |
| Models | `ExploreModels.kt` | Basic - missing promoted trainers |
| Navigation | `MainActivity.kt` | Routes exist |
| ModeTabBar | `ModeTabBar.kt` | Has PROGRAMS→"explore" for personal |

### ❌ Missing from Android (Must Implement)

#### Priority P0 - Core Explore Screen Enhancements

1. **Sliding Segmented Control** (Trainers/Events tabs)
   - iOS: `ExploreSlidingSegment` component with capsule animation
   - Android: Custom composable with `animateContentSize`/`animateColorAsState`

2. **Spotlight Hero Card** (Featured Specialist)
   - iOS: `TrainerSpotlightHeroCard` with gradient, glow ring, "SPOTLIGHT SPECIALIST" badge
   - Android: Custom composable with gradient background, profile glow

3. **Trending Tags View**
   - iOS: `TrendingTagsView` with #hashtag style chips
   - Android: Horizontal scroll of styled chips

4. **Ziro Recommends Section**
   - iOS: Separate section between Featured and Nearby
   - Android: New section with promoted trainers from API

5. **Map Spotlight Preview Card**
   - iOS: `MapSpotlightPreviewCard` with gradient, coach count
   - Android: Composable teaser card

6. **Floating City Header** with search + map buttons
   - iOS: `ExploreCityHeader` with `.ultraThinMaterial` background
   - Android: `Row` with translucent background, overlay on scroll

7. **City Picker Sheet**
   - iOS: `CityPickerSheet` with current location + city list
   - Android: `ModalBottomSheet` with radio list

8. **Pull-to-refresh** on main explore
   - iOS: `.refreshable { viewModel.loadData() }`
   - Android: `pullToRefresh` modifier

9. **Sample Showcase Events** (premium mock events)
   - iOS: `getSampleShowcaseEvents()` with brand/corporate/gym events
   - Android: Similar preview data or remove (if data exists)

#### Priority P1 - Discovery Screen Enhancements

10. **Events section in Discovery** (.all type)
    - iOS: Shows both trainers and events sections
    - Android: Already partially done, verify

11. **Infinite scroll pagination** in Discovery
    - iOS: `loadMore()` triggered on last item appear
    - Android: `LaunchedEffect` for load-more trigger

12. **Local fuzzy search fallback**
    - iOS: `preSearchTrainers`/`preSearchEvents` cache for offline matching
    - Android: Cache results for local filtering

13. **Organizer type badges** (brand/corporate/gym)
    - iOS: Color-coded badges with icons
    - Android: Add to event cards

14. **Event highlights tags**
    - iOS: Blue background chips ("Free Tee", "Live DJ")
    - Android: Add to event rows

15. **Capacity indicator bar**
    - iOS: Progress bar with color change at 80%
    - Android: Add to event cards

16. **"Selling out" badge**
    - iOS: Orange badge on near-capacity events
    - Android: Add to event items

#### Priority P2 - Map Screen Enhancements

17. **Event annotations on map** (not just trainers)
    - iOS: Both `.trainer` and `.event` types
    - Android: Need to add event markers

18. **Map filter modes**
    - iOS: `MapFilterMenu` with All/Trainers/Events/Yoga
    - Android: Filter overlay/fab menu

19. **Search results** on map (locations/specialists/events)
    - iOS: Integrated search bar with sections
    - Android: Search bar with places autocomplete

20. **Cluster list view with detail**
    - iOS: `ClusterListView` with items list
    - Android: Enhancement to bottom sheet

#### Priority P3 - Event Detail Enhancements

21. **Host info with avatar**
    - iOS: Hosted by "Name" section
    - Android: Add host card

22. **Map for event location**
    - iOS: Inline MapKit view
    - Android: Static map or Google Maps intent

23. **Success alert on enrollment**
    - iOS: Alert with auto-dismiss
    - Android: Snackbar/dialog

24. **Paid event checkout flow**
    - iOS: SafariView with Stripe URL
    - Android: Custom Tab / WebView

#### Priority P4 - Data Layer Enhancements

25. **PromotedTrainer model + API**
    - iOS: `PromotedTrainer` → `toTrainerPublicProfile()`
    - Android: New model + API method + repository

26. **Event highlights + organizerType fields**
    - iOS: `highlights: [String]?`, `organizerType: String?`
    - Android: Already in model? Verify ExploreEvent model

27. **Notification subscription** for events
    - iOS: `subscribeToEventNotifications()` via API
    - Android: Push notification topic subscription

28. **Initial filter passing from onboarding**
    - iOS: `appState.initialTrainerFilters` passed to explore
    - Android: Already has specialty/location params in route

---

## 3. Implementation Order

### Phase 1: Core Explore Screen (P0) — ~5 days
```
1. Upgrade ExploreViewModel (promoted trainers, recommended, nearby, location)
2. Rewrite ExploreScreen with sliding segments (Trainers/Events)
3. Spotlight Hero Card composable
4. Trending Tags composable
5. Ziro Recommends section
6. Map Spotlight Preview Card
7. Floating City Header (overlay)
8. City Picker bottom sheet
9. Pull-to-refresh
```

### Phase 2: Discovery Enhancements (P1) — ~3 days
```
10. Events section in discovery (.all type) — verify/fix
11. Infinite scroll pagination in TrainerDiscoveryViewModel
12. Local fuzzy search fallback
13. Organizer type badges on event cards
14. Event highlights tags
15. Capacity indicator bar
16. "Selling out" badge
```

### Phase 3: Map & Event Detail (P2-P3) — ~4 days
```
17. Event markers on map
18. Map filter modes
19. Map search results UI
20. Host info section in EventDetailScreen
21. Event location map
22. Success alert on enrollment
23. Paid checkout flow
```

### Phase 4: Data Layer & Polish (P4) — ~2 days
```
24. PromotedTrainer model + API integration
25. Notification subscription button
26. Verify initial filter passing from onboarding
27. Remove sample/mock events or make dynamic
```

---

## 4. Detailed File-by-File Change Plan

### Phase 1 — ExploreViewModel.kt (MAJOR REWRITE)
Current: 93 lines → Target: ~300 lines

**Add state:**
- `userLocationCity: String?`
- `nearbyTrainers: List<TrainerSummary>`
- `recommendedTrainers: List<TrainerSummary>` (Promoted → ZIRO_RECOMMENDED)
- `sortedDates: List<String>` for date-grouped events
- `isSubscribing: Boolean`
- `notificationSubscriptionSuccess: Boolean`
- `initialFilters: (specialty, location, lat, lon)?`

**Add methods:**
- `loadContent()` — parallel fetch for all data sections
- `selectCity(ExploreCity)` — already exists, enhance
- `subscribeToEventNotifications()`
- `loadEventsList()` — separate function for category changes
- Location management (combine with Android LocationManager)

### Phase 1 — ExploreScreen.kt (MAJOR REWRITE)
Current: 718 lines → Target: ~900 lines

**New structure:**
```
ExploreScreen
├── Scaffold
│   ├── [Floating Header Overlay]  ExploreCityHeader
│   └── LazyColumn
│       ├── ExploreSlidingSegment (custom segmented control)
│       ├── [if trainers tab]
│       │   ├── TrainerSpotlightHeroCard
│       │   ├── BrowseByCategory + TrendingTags
│       │   ├── TrainersNearYou (LazyRow)
│       │   ├── FeaturedTrainers (LazyRow)
│       │   ├── ZiroRecommends (LazyRow)
│       │   └── MapSpotlightPreviewCard
│       ├── [if events tab]
│       │   ├── FeaturedEvents (carousel LazyRow)
│       │   ├── [if empty] ExploreEmptyEventsView
│       │   └── UpcomingEvents (grouped by date)
│       └── pullToRefresh
├── [Sheet] CityPickerSheet
├── [Sheet] TrainerDiscoveryScreen
├── [Sheet] TrainerMapScreen
├── [Sheet] EventDetailScreen
└── [Sheet] TrainerPublicProfileScreen
```

**New composables to create:**
- `ExploreSlidingSegment` — capsule-style segmented control
- `TrainerSpotlightHeroCard` — gradient, glow ring, spotlight badge
- `TrendingTagsRow` — hashtag chips
- `ExploreCityHeader` — floating overlay with city/search/map
- `CityPickerSheet` — ModalBottomSheet
- `MapSpotlightPreviewCard` — gradient teaser card
- `ExploreEmptyTrainersView` — empty state with icon
- `ExploreEmptyEventsView` — empty state with "Notify Me" button

### Phase 2 — TrainerDiscoveryViewModel.kt (ENHANCE)
Current: 144 lines → Target: ~250 lines

**Add:**
- Pagination state: `currentPage`, `canLoadMore`, `pageSize`, `totalPages`
- `loadMore()` method
- Local cache for fuzzy search fallback
- Events type filter for `.all` mode (verify existing)
- Organizer type support

### Phase 2 — TrainerDiscoveryScreen.kt (ENHANCE)
Current: 463 lines

**Add:**
- Infinite scroll (loadMore on last item appear)
- Events section when type == ALL (already partially done — verify)
- Event cards with organizer badges, highlights, capacity, selling-out badges
- Enhancement to the filter sheet

### Phase 2 — Event cards (ExploreScreen.kt & TrainerDiscoveryScreen.kt)
**Enhance existing composables:**
- `InteractiveEventCard` — add organizer type badge, highlights, capacity bar
- `CompactEventCard` — add "selling out" badge, capacity
- `ExploreEventRow` equivalent for discovery screen

### Phase 3 — TrainerMapScreen.kt (ENHANCE)
Current: 411 lines → Target: ~600 lines

**Add:**
- Event markers (not just trainers)
- `MapFilterMode` enum + filter menu
- Search bar with location/specialists/events sections
- `EventMapCard` composable for selected event
- `ClusterListView` for multi-item clusters

### Phase 3 — EventDetailScreen.kt (ENHANCE)
Current: 238 lines

**Add:**
- Host info card with avatar
- Inline map or map placeholder
- Success alert/dialog on enrollment
- Paid checkout flow (Custom Chrome Tab)

### Phase 4 — Models & Data Layer
**ExploreModels.kt:**
- Add `PromotedTrainer` data class
- Add `PromotedTrainersResponse` wrapper
- Add `EventBookingResponse`, `CheckoutSessionResponse` if needed
- Verify event.highlights and organizerType fields exist

**ExploreRepository.kt:**
- Add `getPromotedTrainers(category: String?)` method
- Add `subscribeToNotifications(topic: String?)` method
- Add `createCheckoutSession(eventId: String)` or use existing BillingRepository

**ZiroApi.kt:**
- Add `getPromotedTrainers()` Retrofit method
- Add notification subscription endpoint (verify exists)
- Verify all query param fields match iOS API

---

## 5. Key Design Decisions

### 5.1 Tab Structure
**Decision:** Add sliding segmented control (Trainers/Events) INSIDE the Explore screen, rather than using the bottom tab bar. This matches iOS exactly.

### 5.2 Sheet vs Full-Screen Navigation
**Decision:** Use `ModalBottomSheet` for city picker, filter sheets. Use full routes for EventDetail, TrainerProfile, Discovery, Map. This aligns with Android convention while matching iOS interaction flow.

### 5.3 Promoted Trainers API Endpoint
The iOS app uses `GET /api/explore/promoted-trainers?category=FEATURED` and `GET /api/explore/promoted-trainers?category=ZIRO_RECOMMENDED`. The Android app currently uses `GET /api/explore/featured` which returns both. Need to align.

### 5.4 Sample Showcase Events
iOS has hardcoded sample events (Reebok HIIT, Ziro Yoga, Gold's Gym Strength) injected for demo purposes. Android can either replicate these or conditionally show them only in debug mode.

### 5.5 Location Handling
iOS uses `LocationManager.shared` (Combine-based). Android needs to use Fused Location Provider via a Hilt-injectable manager. The ExploreViewModel should observe location changes and reload data.

### 5.6 Pagination Pattern
iOS uses manual page tracking. Android should use the same pattern (`currentPage`, `canLoadMore`, `loadMore()`) for consistency.

---

## 6. Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Promoted trainers API may differ | Blocks featured/recommended sections | Verify API response format; adapt Android model |
| Location permissions | Can't show "Near You" | Handle gracefully with fallback |
| Org type/highlights fields may not be in API | Event cards degrade | Make fields nullable, graceful fallback |
| Push notification topic subscription API may not exist | "Notify Me" disabled | Hide button if API 404's |
| Map performance on low-end devices | Map screen lag | Use cluster limit, marker caching |

---

## 7. File Inventory

### Files to CREATE (11)

| File | Purpose | Priority |
|------|---------|----------|
| `ui/discovery/components/ExploreSlidingSegment.kt` | Capsule segmented control | P0 |
| `ui/discovery/components/TrainerSpotlightHeroCard.kt` | Premium hero card | P0 |
| `ui/discovery/components/TrendingTagsRow.kt` | Trending search tags | P0 |
| `ui/discovery/components/MapSpotlightPreviewCard.kt` | Map teaser card | P0 |
| `ui/discovery/components/ExploreCityHeader.kt` | Floating header overlay | P0 |
| `ui/discovery/components/CityPickerSheet.kt` | City selection bottom sheet | P0 |
| `ui/discovery/components/ExploreEmptyTrainersView.kt` | Empty trainers state | P0 |
| `ui/discovery/components/ExploreEmptyEventsView.kt` | Empty events state | P0 |
| `ui/discovery/components/EventMapCard.kt` | Map event card | P2 |
| `ui/discovery/components/MapFilterMenu.kt` | Map filter overlay | P2 |
| `model/PromotedTrainer.kt` | Promoted trainer model | P4 |

### Files to MODIFY (15)

| File | Change | Priority |
|------|--------|----------|
| `ui/discovery/ExploreScreen.kt` | Full rewrite with segments, hero, tags, sections | P0 |
| `viewmodel/ExploreViewModel.kt` | Add location, promoted trainers, subscriptions | P0 |
| `ui/discovery/TrainerDiscoveryScreen.kt` | Add events in .all, pagination, badges | P1 |
| `viewmodel/TrainerDiscoveryViewModel.kt` | Add pagination, fuzzy cache | P1 |
| `ui/discovery/TrainerMapScreen.kt` | Add events, filters, search | P2 |
| `ui/discovery/EventDetailScreen.kt` | Add host, map, checkout | P3 |
| `viewmodel/EventDetailViewModel.kt` | Add paid checkout | P3 |
| `ui/discovery/EventsListScreen.kt` | Add date headers, free filter | P1 |
| `viewmodel/EventsViewModel.kt` | Add date grouping | P1 |
| `model/ExploreModels.kt` | Add PromotedTrainer, verify highlights/orgType | P4 |
| `data/repository/ExploreRepository.kt` | Add promoted trainers, subscriptions | P4 |
| `data/remote/ZiroApi.kt` | Add promoted trainers endpoint | P4 |
| `ui/theme/Theme.kt` | Add new color values | P0 |
| `MainActivity.kt` | Verify nav routes | P0 |

---

## 8. Effort Estimation

| Phase | Files | Estimated Time |
|-------|-------|----------------|
| P0: Core Explore Redesign | 6 files (3 new, 3 modified) | 4-5 days |
| P1: Discovery Enhancements | 4 files | 2-3 days |
| P2-P3: Map & Event Detail | 4 files | 3-4 days |
| P4: Data Layer & Polish | 5 files | 1-2 days |
| **Total** | **~19 files** | **10-14 days** |
