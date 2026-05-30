# T26 Project Overview

The backend mapper layer currently mixes:

- simple annotation-based mappers
- XML-backed dynamic query mappers
- repeated result mapping definitions in both styles

T26 does not aim to standardize everything onto one mechanism. Instead, it narrows on duplicated mapper
fragments that increase maintenance cost while providing no behavioral value.
