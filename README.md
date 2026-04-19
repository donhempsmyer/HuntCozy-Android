# HuntCozy

HuntCozy is an intelligent gear recommendation engine for hunters. By analyzing real-time weather data and considering specific hunting styles and weapon choices, HuntCozy ensures you stay comfortable and prepared in the field.

## Features

- **Smart Gear Recommendations**: Recommends the ideal layering system based on temperature bands (Hot, Mild, Cool, Cold, Very Cold).
- **Hunting Style Integration**: Adjusts equipment lists for different hunting methods like Treestand, Stalking, or Blind hunting.
- **Weapon-Specific Accessories**: Automatically includes necessary gear for your selected weapon (Bow, Rifle, Crossbow, etc.).
- **Closet Management**: Synchronize your personal hunting gear using Firebase Firestore.
- **Weather Integration**: Real-time weather data fetching via OpenMeteo API.
- **MVVM Architecture**: Clean, maintainable code using LiveData and ViewModel.

## Tech Stack

- **Language**: Java (Android)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Backend**: Firebase Authentication & Cloud Firestore
- **Networking**: Retrofit 2 for Weather APIs
- **Data Visualization**: MPAndroidChart
- **UI Components**: Material Components for Android

## Project Structure

- `dev.donhempsmyer.huntcozy.algo`: Core recommendation logic (`GearRecommender`).
- `dev.donhempsmyer.huntcozy.data`: Repository and Data Source patterns.
- `dev.donhempsmyer.huntcozy.ui`: UI Fragments and ViewModels.
- `dev.donhempsmyer.huntcozy.model`: Data models for Closet items, Weather, and Gear.

## Getting Started

1. Clone the repository.
2. Add your `google-services.json` to the `app/` directory.
3. Build the project using Android Studio.
4. (Optional) Configure your OpenMeteo API settings in the weather repository.

---
_HuntCozy - Stay Comfortable. Stay Focused._
