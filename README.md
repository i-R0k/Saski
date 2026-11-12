# Gaba – Leki (Android, 2026)

Natywna aplikacja Android do rejestrowania dawek leków królika Gaby oraz obserwacji apetytu, kału, moczu i wagi.

Dane są przechowywane **lokalnie w telefonie**, bez konta i bez internetu. Działa od Androida 8.0.

## Zalecenia z wizyty 11.11.2026

- Bactrim 1 ml doustnie 2 × dziennie
- Pyralgin 0,16 ml doustnie 2 × dziennie
- Tralieve 20 mg: ¾ tabletki doustnie 2 × dziennie
- Gasprid 5 mg: ½ tabletki doustnie 2 × dziennie
- Mirtagen: ⅓ tabletki doustnie 1 × dziennie
- Enterozoo 2 ml 2 × dziennie, przynajmniej godzina od innych leków doustnych
- Flora Care 1 kapsułka 1 × dziennie, przynajmniej godzina od Bactrimu

Przykładowe godziny: 08:00 / 10:00 / 12:00 / 20:00 / 22:00. Godziny można zmieniać; aplikacja sprawdza odstępy. Leki od wieczora 11.11.2026 do kontroli według zaleceń.

Kontrola 15–16.11.2026, a przy pogorszeniu pilna konsultacja. Jeśli pogorszenie: dokarmianie 15–20 ml karmy ratunkowej 3–5 × dziennie, zgodnie z pisemnymi zaleceniami lekarza.

## Budowanie

`gradle :app:assembleDebug` przy Java 17, Android SDK 35 i Gradle 8.9.

Branch `gaba-med-2026-11` zawiera automatyczny workflow GitHub Actions tworzący APK.

**Uwaga o aktualizacjach:** nowa aplikacja ma identyfikator `pl.saski.gaba`, więc nie nadpisuje wcześniejszej `pl.saski.leki`. APK debug podpisywane na różnych komputerach mogą mieć różne certyfikaty; przed reinstalacją należy wyeksportować dane. Do kolejnych wersji produkcyjnych zalecane jest stałe prywatne podpisywanie release.
