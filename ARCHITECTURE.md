# DentBridge: Enterprise Dental Clinic & Laboratory Management Platform
## Product Architecture Blueprint & Technical Specifications

This document defines the production-ready technical architecture, directory structure, data models, and configurations for **DentBridge**—a world-class, multi-tenant dental ecosystem connecting Dental Clinics and Dental Laboratories.

---

## Table of Contents
1. [Scalable Folder Structure (Feature-First)](#1-scalable-folder-structure-feature-first)
2. [Package List (`package.json`)](#2-package-list-packagejson)
3. [Firebase Configuration & Initialization](#3-firebase-configuration--initialization)
4. [Environment Variables & Type-Safe Config Loader](#4-environment-variables--type-safe-config-loader)
5. [Theme System (Material Design 3 & Type-Safe Theme)](#5-theme-system-material-design-3--type-safe-theme)
6. [Navigation System & Routing Layout (Expo Router)](#6-navigation-system--routing-layout-expo-router)
7. [Authentication Flow & Zustand Store](#7-authentication-flow--zustand-store)
8. [Error Handling Strategy & Custom Classes](#8-error-handling-strategy--custom-classes)
9. [Logging Strategy (Structured Console/Sentry Log Service)](#9-logging-strategy-structured-consolesentry-log-service)
10. [Offline Strategy & Cache Persistence](#10-offline-strategy--cache-persistence)
11. [State Management Strategy (Client vs. Server State)](#11-state-management-strategy-client-vs-server-state)
12. [Database Architecture & Mappings](#12-database-architecture--mappings)
13. [Firestore Collections Schemas](#13-firestore-collections-schemas)
14. [Production Security Rules (Firestore & Storage)](#14-production-security-rules-firestore--storage)
15. [Naming Conventions](#15-naming-conventions)
16. [Coding Standards (ESLint & Prettier Configurations)](#16-coding-standards-eslint--prettier-configurations)
17. [Scalability Plan & High-Throughput Strategy](#17-scalability-plan--high-throughput-strategy)
18. [Structured Development Roadmap](#18-structured-development-roadmap)

---

## 1. Scalable Folder Structure (Feature-First)

The architecture is structured using **Feature-First Architecture** to isolate business domains, improve maintainability, and allow parallel development across teams.

```
/
├── .env.example
├── .env.production
├── .env.staging
├── .env.development
├── package.json
├── tsconfig.json
├── app.json
├── firebase.json
├── firestore.rules
├── storage.rules
├── src/
│   ├── config/                     # Application configurations & Env loaders
│   │   ├── env.ts
│   │   └── firebase.ts
│   ├── theme/                      # MD3 Design System tokens and custom extensions
│   │   ├── colors.ts
│   │   ├── typography.ts
│   │   ├── spacing.ts
│   │   └── index.ts
│   ├── navigation/                 # Navigation helpers, links, types
│   │   └── types.ts
│   ├── core/                       # Shared platform utilities and services
│   │   ├── errors/                 # Global error management
│   │   │   ├── DentBridgeError.ts
│   │   │   └── ErrorHandler.ts
│   │   ├── logging/                # Multi-channel logger service
│   │   │   └── Logger.ts
│   │   ├── offline/                # Offline sync engine and AsyncStorage helpers
│   │   │   ├── NetInfoProvider.tsx
│   │   │   └── OfflinePersister.ts
│   │   ├── api/                    # Core React Query client and Fetch wrappers
│   │   │   └── queryClient.ts
│   │   └── state/                  # Shared system stores (Zustand)
│   │       ├── useAuthStore.ts
│   │       └── useUIStore.ts
│   ├── components/                 # Global, cross-domain reusable Atom/Molecule UI components
│   │   └── common/
│   │       ├── Button.tsx
│   │       ├── Card.tsx
│   │       ├── Input.tsx
│   │       └── Toast.tsx
│   └── features/                   # Core Domain-Driven Feature Modules
│       ├── auth/                   # Identity Access Management (IAM)
│       │   ├── api/
│       │   │   └── authService.ts
│       │   ├── components/
│       │   │   └── LoginForm.tsx
│       │   └── hooks/
│       │       └── useAuth.ts
│       ├── cases/                  # Work Orders, Prescription Forms, Milestones
│       │   ├── api/
│       │   │   └── casesApi.ts
│       │   ├── components/
│       │   │   ├── CaseCard.tsx
│       │   │   └── PrescriptionForm.tsx
│       │   ├── hooks/
│       │   │   └── useCases.ts
│       │   └── types/
│       │       └── index.ts
│       ├── cad-viewer/             # 3D STL mesh viewing & metadata parser
│       │   ├── api/
│       │   │   └── stlLoader.ts
│       │   ├── components/
│       │   │   └── STLViewer.tsx
│       │   └── hooks/
│       │       └── useSTLParser.ts
│       ├── messaging/              # Real-time chat & Collaboration channel
│       │   ├── api/
│       │   │   └── chatApi.ts
│       │   ├── components/
│       │   │   └── ChatWindow.tsx
│       │   └── hooks/
│       │       └── useChat.ts
│       └── notifications/          # In-app alerts, FCM tokens management
│           ├── api/
│           │   └── fcmService.ts
│           └── hooks/
│               └── useNotifications.ts
├── app/                            # Expo Router Layouts & Routing Hierarchy
│   ├── _layout.tsx                 # Root layout (QueryClientProvider, PaperProvider, AuthGuard)
│   ├── index.tsx                   # Splash / Route redirector
│   ├── (auth)/                     # Auth Sub-navigation stack
│   │   ├── login.tsx
│   │   ├── register.tsx
│   │   └── forgot-password.tsx
│   ├── (clinic)/                   # Clinic Portal Layout (Dentists & Admins)
│   │   ├── _layout.tsx
│   │   ├── dashboard.tsx
│   │   ├── cases/
│   │   │   ├── index.tsx
│   │   │   ├── [id].tsx
│   │   │   └── create.tsx
│   │   └── settings.tsx
│   ├── (lab)/                      # Lab Portal Layout (Lab Admins & Techs)
│   │   ├── _layout.tsx
│   │   ├── dashboard.tsx
│   │   ├── cases/
│   │   │   ├── index.tsx
│   │   │   └── [id].tsx
│   │   └── settings.tsx
│   └── shared/                     # Multi-role Shared Screens
│       ├── chat/[channelId].tsx
│       └── viewer/[fileId].tsx
```

---

## 2. Package List (`package.json`)

Production-ready package definition lockups, balancing Firebase, routing, forms, validation, and layout hooks.

```json
{
  "name": "dentbridge-app",
  "version": "1.0.0",
  "scripts": {
    "start": "expo start",
    "android": "expo start --android",
    "ios": "expo start --ios",
    "web": "expo start --web",
    "ts:check": "tsc",
    "lint": "eslint \"src/**/*.{ts,tsx}\" \"app/**/*.{ts,tsx}\"",
    "format": "prettier --write \"src/**/*.{ts,tsx}\" \"app/**/*.{ts,tsx}\""
  },
  "dependencies": {
    "@react-native-async-storage/async-storage": "1.23.1",
    "@react-native-community/netinfo": "11.3.1",
    "@react-navigation/native": "6.1.17",
    "@tanstack/react-query": "5.35.1",
    "@tanstack/react-query-persist-client": "5.35.1",
    "expo": "~51.0.0",
    "expo-application": "~5.9.1",
    "expo-constants": "~16.0.1",
    "expo-device": "~6.0.1",
    "expo-file-system": "~17.0.1",
    "expo-image": "~1.12.11",
    "expo-linking": "~6.3.1",
    "expo-notifications": "~0.28.1",
    "expo-router": "~3.5.14",
    "expo-secure-store": "~13.0.1",
    "expo-status-bar": "~1.12.1",
    "firebase": "^10.12.0",
    "react": "18.2.0",
    "react-hook-form": "^7.51.4",
    "react-native": "0.74.1",
    "react-native-gesture-handler": "~2.16.1",
    "react-native-paper": "^5.12.3",
    "react-native-reanimated": "~3.10.1",
    "react-native-safe-area-context": "4.10.1",
    "react-native-screens": "3.31.1",
    "react-native-svg": "15.2.0",
    "react-native-vector-icons": "^10.1.0",
    "zod": "^3.23.8",
    "zustand": "^4.5.2"
  },
  "devDependencies": {
    "@babel/core": "^7.20.0",
    "@types/react": "~18.2.45",
    "@typescript-eslint/eslint-plugin": "^7.8.0",
    "@typescript-eslint/parser": "^7.8.0",
    "eslint": "^8.57.0",
    "eslint-config-prettier": "^9.1.0",
    "eslint-plugin-react": "^7.34.1",
    "eslint-plugin-react-hooks": "^4.6.2",
    "prettier": "^3.2.5",
    "typescript": "~5.3.3"
  },
  "private": true
}
```

---

## 3. Firebase Configuration & Initialization

Optimized Firebase SDK integration offering robust native performance, explicit Offline Cache structures, and clear routing paths for local emulators in local/development profiles.

```typescript
// src/config/firebase.ts
import { initializeApp, getApps, getApp, FirebaseApp } from 'firebase/app';
import { 
  initializeAuth, 
  getReactNativePersistence, 
  Auth 
} from 'firebase/auth';
import { 
  initializeFirestore, 
  persistentLocalCache, 
  persistentMultipleTabManager,
  Firestore,
  connectFirestoreEmulator
} from 'firebase/firestore';
import { getStorage, FirebaseStorage, connectStorageEmulator } from 'firebase/storage';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { env } from './env';
import { Logger } from '../core/logging/Logger';

const firebaseConfig = {
  apiKey: env.FIREBASE_API_KEY,
  authDomain: env.FIREBASE_AUTH_DOMAIN,
  projectId: env.FIREBASE_PROJECT_ID,
  storageBucket: env.FIREBASE_STORAGE_BUCKET,
  messagingSenderId: env.FIREBASE_MESSAGING_SENDER_ID,
  appId: env.FIREBASE_APP_ID,
  measurementId: env.FIREBASE_MEASUREMENT_ID,
};

let app: FirebaseApp;
let auth: Auth;
let db: Firestore;
let storage: FirebaseStorage;

try {
  if (getApps().length === 0) {
    app = initializeApp(firebaseConfig);
    Logger.info('Firebase', 'Core SDK initialized successfully.');
  } else {
    app = getApp();
  }

  // Initialize Auth with AsyncStorage Persistence
  auth = initializeAuth(app, {
    persistence: getReactNativePersistence(AsyncStorage),
  });
  Logger.info('Firebase', 'Auth persistent layer established.');

  // Initialize Firestore with Enterprise Multi-Tab Persistent Offline Cache
  db = initializeFirestore(app, {
    localCache: persistentLocalCache({
      tabManager: persistentMultipleTabManager(),
    }),
  });
  Logger.info('Firebase', 'Firestore cache layer configured.');

  storage = getStorage(app);

  // Hook Dev Emulators if flagged in Environment Variables
  if (env.EXPO_PUBLIC_USE_EMULATORS) {
    Logger.warn('Firebase', 'Connecting to local Firebase Emulators...');
    connectFirestoreEmulator(db, 'localhost', 8080);
    connectStorageEmulator(storage, 'localhost', 9199);
  }

} catch (error) {
  Logger.error('Firebase', 'Failed to initialize Firebase ecosystem.', error as Error);
  throw error;
}

export { app, auth, db, storage };
```

---

## 4. Environment Variables & Type-Safe Config Loader

Strictly enforced runtime environment validation using `zod`. This prevents app execution if critical security keys or endpoint references are absent or malformed.

### `.env.example`
```env
EXPO_PUBLIC_ENV=development
EXPO_PUBLIC_USE_EMULATORS=false
EXPO_PUBLIC_FIREBASE_API_KEY=AIzaSyA1...
EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN=dentbridge-prod.firebaseapp.com
EXPO_PUBLIC_FIREBASE_PROJECT_ID=dentbridge-prod
EXPO_PUBLIC_FIREBASE_STORAGE_BUCKET=dentbridge-prod.appspot.com
EXPO_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=1234567890
EXPO_PUBLIC_FIREBASE_APP_ID=1:123:web:abc
EXPO_PUBLIC_FIREBASE_MEASUREMENT_ID=G-ABCDEF
EXPO_PUBLIC_SENTRY_DSN=https://sentry...
```

### Type-Safe Loader Module
```typescript
// src/config/env.ts
import { z } from 'zod';

const envSchema = z.object({
  ENV: z.enum(['development', 'staging', 'production']).default('development'),
  EXPO_PUBLIC_USE_EMULATORS: z.preprocess((val) => val === 'true', z.boolean()).default(false),
  FIREBASE_API_KEY: z.string().min(1, 'FIREBASE_API_KEY is required'),
  FIREBASE_AUTH_DOMAIN: z.string().min(1, 'FIREBASE_AUTH_DOMAIN is required'),
  FIREBASE_PROJECT_ID: z.string().min(1, 'FIREBASE_PROJECT_ID is required'),
  FIREBASE_STORAGE_BUCKET: z.string().min(1, 'FIREBASE_STORAGE_BUCKET is required'),
  FIREBASE_MESSAGING_SENDER_ID: z.string().min(1, 'FIREBASE_MESSAGING_SENDER_ID is required'),
  FIREBASE_APP_ID: z.string().min(1, 'FIREBASE_APP_ID is required'),
  FIREBASE_MEASUREMENT_ID: z.string().optional(),
  SENTRY_DSN: z.string().url('SENTRY_DSN must be a valid URL').optional(),
});

function loadEnv() {
  const result = envSchema.safeParse({
    ENV: process.env.NODE_ENV,
    EXPO_PUBLIC_USE_EMULATORS: process.env.EXPO_PUBLIC_USE_EMULATORS,
    FIREBASE_API_KEY: process.env.EXPO_PUBLIC_FIREBASE_API_KEY,
    FIREBASE_AUTH_DOMAIN: process.env.EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN,
    FIREBASE_PROJECT_ID: process.env.EXPO_PUBLIC_FIREBASE_PROJECT_ID,
    FIREBASE_STORAGE_BUCKET: process.env.EXPO_PUBLIC_FIREBASE_STORAGE_BUCKET,
    FIREBASE_MESSAGING_SENDER_ID: process.env.EXPO_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
    FIREBASE_APP_ID: process.env.EXPO_PUBLIC_FIREBASE_APP_ID,
    FIREBASE_MEASUREMENT_ID: process.env.EXPO_PUBLIC_FIREBASE_MEASUREMENT_ID,
    SENTRY_DSN: process.env.EXPO_PUBLIC_SENTRY_DSN,
  });

  if (!result.success) {
    console.error('❌ Invalid environment configuration:', result.error.format());
    throw new Error('Environment configuration validation failed.');
  }

  return result.data;
}

export const env = loadEnv();
```

---

## 5. Theme System (Material Design 3 & Type-Safe Theme)

A clean Material Design 3 theme system based on `react-native-paper`, supporting dark/light variants, custom functional tokens (success, warning, dental shade tints), and high contrast variables.

```typescript
// src/theme/colors.ts
export const LightColors = {
  primary: '#1A365D',         // Deep Royal Blue (Dental Trust)
  onPrimary: '#FFFFFF',
  primaryContainer: '#D6E4FF',
  onPrimaryContainer: '#001B44',
  secondary: '#0D9488',       // Teal Accent (Laboratory precision, clean environment)
  onSecondary: '#FFFFFF',
  secondaryContainer: '#CCFBF1',
  onSecondaryContainer: '#00201D',
  background: '#F8FAFC',      // Off-White slate
  onBackground: '#0F172A',
  surface: '#FFFFFF',
  onSurface: '#0F172A',
  surfaceVariant: '#E2E8F0',
  onSurfaceVariant: '#475569',
  outline: '#94A3B8',
  error: '#EF4444',
  onError: '#FFFFFF',
  errorContainer: '#FEE2E2',
  onErrorContainer: '#450A0A',
  
  // Custom non-MD3 brand colors
  success: '#10B981',
  warning: '#F59E0B',
  shadeA1: '#FAF5FF',         // Pale purple tint for dental shade visualization
  shadeA2: '#F3E8FF',
  shadeB1: '#ECFDF5',
};

export const DarkColors = {
  primary: '#ADC6FF',
  onPrimary: '#002E68',
  primaryContainer: '#004494',
  onPrimaryContainer: '#D6E4FF',
  secondary: '#5EEAD4',
  onSecondary: '#003731',
  secondaryContainer: '#005047',
  onSecondaryContainer: '#CCFBF1',
  background: '#0F172A',      // Slate 900
  onBackground: '#F8FAFC',
  surface: '#1E293B',
  onSurface: '#F8FAFC',
  surfaceVariant: '#334155',
  onSurfaceVariant: '#94A3B8',
  outline: '#64748B',
  error: '#FCA5A5',
  onError: '#600505',
  errorContainer: '#7F1D1D',
  onErrorContainer: '#FEE2E2',

  // Custom non-MD3 brand colors
  success: '#34D399',
  warning: '#FBBF24',
  shadeA1: '#1E1B4B',
  shadeA2: '#311042',
  shadeB1: '#022C22',
};
```

```typescript
// src/theme/typography.ts
import { TextStyle } from 'react-native';

interface FontStyles {
  h1: TextStyle;
  h2: TextStyle;
  bodyLarge: TextStyle;
  bodyMedium: TextStyle;
  caption: TextStyle;
  mono: TextStyle;
}

export const Typography: FontStyles = {
  h1: {
    fontFamily: 'System',
    fontSize: 28,
    fontWeight: '700',
    lineHeight: 34,
    letterSpacing: -0.5,
  },
  h2: {
    fontFamily: 'System',
    fontSize: 22,
    fontWeight: '600',
    lineHeight: 28,
  },
  bodyLarge: {
    fontFamily: 'System',
    fontSize: 16,
    fontWeight: '400',
    lineHeight: 22,
  },
  bodyMedium: {
    fontFamily: 'System',
    fontSize: 14,
    fontWeight: '400',
    lineHeight: 20,
  },
  caption: {
    fontFamily: 'System',
    fontSize: 12,
    fontWeight: '500',
    lineHeight: 16,
    color: '#64748B',
  },
  mono: {
    fontFamily: 'System', // Fallback to system monospace where applicable
    fontSize: 13,
    fontWeight: '400',
    letterSpacing: -0.2,
  }
};
```

```typescript
// src/theme/index.ts
import { MD3LightTheme, MD3DarkTheme, configureFonts } from 'react-native-paper';
import { LightColors, DarkColors } from './colors';
import { Typography } from './typography';

const fontConfig = {
  customVariant: {
    fontFamily: 'System',
    fontWeight: '400' as const,
    letterSpacing: 0.5,
    lineHeight: 16,
    fontSize: 12,
  }
};

export const AppLightTheme = {
  ...MD3LightTheme,
  colors: {
    ...MD3LightTheme.colors,
    ...LightColors,
  },
  fonts: configureFonts({ config: fontConfig }),
  roundness: 12,
};

export const AppDarkTheme = {
  ...MD3DarkTheme,
  colors: {
    ...MD3DarkTheme.colors,
    ...DarkColors,
  },
  fonts: configureFonts({ config: fontConfig }),
  roundness: 12,
};

export type AppThemeType = typeof AppLightTheme;
```

---

## 6. Navigation System & Routing Layout (Expo Router)

The navigation architecture utilizes **Expo Router (File-based Routing)** with strict folder segmentation for authentication, the multi-tenant clinic portal (Dentist/Clinic Admin), and the laboratory portal (Lab Admin/Technician).

```
app/
├── _layout.tsx                     # Top level orchestration (Theme, State, Core Providers)
├── index.tsx                       # App Entry Redirect (Resolves Auth & Role state)
├── (auth)/                         # IAM Screens
│   ├── _layout.tsx                 # Native Stack Navigation
│   ├── login.tsx                   # Credentials form input (with biometrics flag)
│   ├── register.tsx                # Onboarding multi-role choice
│   └── forgot-password.tsx         # Password reset action
├── (clinic)/                       # Dental Clinic Portal Group
│   ├── _layout.tsx                 # Dynamic Bottom Tab Bar + Safety Rail for Tablets
│   ├── dashboard.tsx               # Analytics, Active Cases count, urgent chat widgets
│   ├── cases/
│   │   ├── index.tsx               # Grid listing of cases, live status filters
│   │   ├── [id].tsx                # Detailed prescription, timeline tracker, STL file list
│   │   └── create.tsx              # Form wizard with dynamic digital scans upload
│   └── settings.tsx                # Clinic user profile, laboratory pairings API key
├── (lab)/                          # Dental Laboratory Portal Group
│   ├── _layout.tsx                 # Dynamic Tab Layout
│   ├── dashboard.tsx               # Operational queues, incoming scanner receipts
│   ├── cases/
│   │   ├── index.tsx               # Advanced search, technician dispatch screen
│   │   └── [id].tsx                # Clinical timeline updates, production stage controller
│   └── settings.tsx                # Material inventory trackers, billing modules
└── shared/                         # Universal collaborative pathways
    ├── chat/[channelId].tsx        # Real-time multi-peer case chats (Firebase synced)
    └── viewer/[fileId].tsx         # 3D WebGL / Canvas STL Scan Mesh Viewer
```

---

## 7. Authentication Flow & Zustand Store

A centralized auth controller managed via a Zustand store, integrating Firebase Authentication and syncing multi-tenant tenant roles on state resolution.

```typescript
// src/core/state/useAuthStore.ts
import { create } from 'zustand';
import { User as FirebaseUser } from 'firebase/auth';

export type UserRole = 'dentist' | 'lab_admin' | 'technician' | 'clinic_admin';

export interface UserProfile {
  uid: string;
  email: string;
  displayName: string;
  role: UserRole;
  organizationId: string;
  organizationName: string;
  createdAt: string;
}

interface AuthState {
  user: FirebaseUser | null;
  profile: UserProfile | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  setSession: (user: FirebaseUser | null, profile: UserProfile | null) => void;
  updateProfile: (profile: Partial<UserProfile>) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  profile: null,
  isLoading: true,
  isAuthenticated: false,
  setSession: (user, profile) => set({
    user,
    profile,
    isAuthenticated: !!user && !!profile,
    isLoading: false,
  }),
  updateProfile: (updates) => set((state) => ({
    profile: state.profile ? { ...state.profile, ...updates } : null
  })),
  logout: () => set({
    user: null,
    profile: null,
    isAuthenticated: false,
    isLoading: false,
  }),
}));
```

---

## 8. Error Handling Strategy & Custom Classes

A unified approach to catching errors, featuring a specialized custom error class hierarchy, visual fallback states, and auto-dispatching to logging aggregators.

```typescript
// src/core/errors/DentBridgeError.ts
export type ErrorSeverity = 'low' | 'medium' | 'high' | 'critical';

export class DentBridgeError extends Error {
  public code: string;
  public severity: ErrorSeverity;
  public userMessage: string;
  public timestamp: Date;

  constructor(
    message: string,
    code = 'GENERIC_ERROR',
    severity: ErrorSeverity = 'medium',
    userMessage = 'An unexpected error occurred. Our engineers have been notified.'
  ) {
    super(message);
    this.name = 'DentBridgeError';
    this.code = code;
    this.severity = severity;
    this.userMessage = userMessage;
    this.timestamp = new Date();
    Object.setPrototypeOf(this, new Target());
  }
}

export class AuthenticationError extends DentBridgeError {
  constructor(message: string, userMessage = 'Login failed. Please verify credentials.') {
    super(message, 'AUTH_FAILED', 'high', userMessage);
  }
}

export class FileUploadError extends DentBridgeError {
  constructor(message: string, userMessage = 'Failed to upload STL model. Check network connectivity.') {
    super(message, 'FILE_UPLOAD_FAILED', 'medium', userMessage);
  }
}
```

```typescript
// src/core/errors/ErrorHandler.ts
import { Alert, Platform } from 'react-native';
import { DentBridgeError, ErrorSeverity } from './DentBridgeError';
import { Logger } from '../logging/Logger';

export class ErrorHandler {
  public static handle(error: Error | DentBridgeError, context = 'AppCore') {
    const isCustom = error instanceof DentBridgeError;
    const severity = isCustom ? (error as DentBridgeError).severity : 'high';
    const userMessage = isCustom ? (error as DentBridgeError).userMessage : 'A system issue was detected.';

    // Log internally
    Logger.error(context, `[${isCustom ? (error as DentBridgeError).code : 'UNCAUGHT'}] ${error.message}`, error);

    // Alert Client on high severity
    if (severity === 'high' || severity === 'critical') {
      this.notifyUser(userMessage);
    }

    // Critical Dispatch to Crashlytics / Sentry
    if (severity === 'critical') {
      this.dispatchToRemoteMonitor(error);
    }
  }

  private static notifyUser(message: string) {
    if (Platform.OS === 'web') {
      alert(message);
    } else {
      Alert.alert(
        'System Notice',
        message,
        [{ text: 'Acknowledge', style: 'cancel' }]
      );
    }
  }

  private static dispatchToRemoteMonitor(error: Error) {
    // Integration Hook for Sentry or custom diagnostic pipe
    Logger.warn('ErrorHandler', `Dispatched critical event to diagnostic server: ${error.name}`);
  }
}
```

---

## 9. Logging Strategy (Structured Console/Sentry Log Service)

Structured logs allow remote tracing, filtering logs by subsystem namespaces, and avoiding memory-leak footprints.

```typescript
// src/core/logging/Logger.ts
type LogNamespace = 'Auth' | 'Firebase' | 'CADViewer' | 'Offline' | 'Messaging' | 'Database' | 'AppCore';

export class Logger {
  private static getTimestamp(): string {
    return new Date().toISOString();
  }

  public static info(namespace: LogNamespace, message: string, ...optionalParams: unknown[]) {
    if (__DEV__) {
      console.log(`[${this.getTimestamp()}] ℹ️ [${namespace}]: ${message}`, ...optionalParams);
    }
  }

  public static warn(namespace: LogNamespace, message: string, ...optionalParams: unknown[]) {
    console.warn(`[${this.getTimestamp()}] ⚠️ [${namespace}]: ${message}`, ...optionalParams);
  }

  public static error(namespace: LogNamespace, message: string, error?: Error, ...optionalParams: unknown[]) {
    console.error(`[${this.getTimestamp()}] ❌ [${namespace}]: ${message}`, error, ...optionalParams);
    // Remote Logging Integrations can be injected here safely
  }
}
```

---

## 10. Offline Strategy & Cache Persistence

DentBridge provides full offline capabilities to accommodate remote environments or local dental operatories. This includes background query syncing and offline state persistence using React Query and NetInfo.

```typescript
// src/core/offline/OfflinePersister.ts
import { QueryClient } from '@tanstack/react-query';
import { persistQueryClient } from '@tanstack/react-query-persist-client';
import { createAsyncStoragePersister } from '@tanstack/query-async-storage-persister';
import AsyncStorage from '@react-native-async-storage/async-storage';

const persister = createAsyncStoragePersister({
  storage: AsyncStorage,
  key: 'DENTBRIDGE_OFFLINE_CACHE',
  throttleTime: 1000,
});

export function configureOfflinePersistence(queryClient: QueryClient) {
  persistQueryClient({
    queryClient,
    persister,
    maxAge: 1000 * 60 * 60 * 24 * 7, // Retain records up to 7 Days
    buster: 'v1_production',
  });
}
```

```typescript
// src/core/offline/NetInfoProvider.tsx
import React, { createContext, useContext, useEffect, useState } from 'react';
import NetInfo from '@react-native-community/netinfo';
import { Logger } from '../logging/Logger';

interface ConnectionContextType {
  isConnected: boolean;
  isInternetReachable: boolean;
}

const ConnectionContext = createContext<ConnectionContextType>({
  isConnected: true,
  isInternetReachable: true,
});

export const NetInfoProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [status, setStatus] = useState<ConnectionContextType>({
    isConnected: true,
    isInternetReachable: true,
  });

  useEffect(() => {
    const unsubscribe = NetInfo.addEventListener((state) => {
      const active = !!state.isConnected;
      const reachable = !!state.isInternetReachable;
      
      setStatus({ isConnected: active, isInternetReachable: reachable });
      Logger.info('Offline', `Network connectivity changed. Online: ${active}, Reachable: ${reachable}`);
    });

    return () => unsubscribe();
  }, []);

  return (
    <ConnectionContext.Provider value={status}>
      {children}
    </ConnectionContext.Provider>
  );
};

export const useConnection = () => useContext(ConnectionContext);
```

---

## 11. State Management Strategy (Client vs. Server State)

State is cleanly decoupled into Server/Cache state (driven by **React Query** for server sync, mutations, and pagination) and transient Client state (driven by **Zustand** for UI configs, local modal selections, active role simulator, and theme preferences).

```typescript
// src/core/api/queryClient.ts
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      gcTime: 1000 * 60 * 60 * 24 * 7, // Garbage Collect cached data after 7 days
      staleTime: 1000 * 60 * 15,        // Keep queries active / unstale for 15 minutes
      refetchOnWindowFocus: false,
      refetchOnReconnect: 'always',
      retry: (failureCount, error) => {
        // Exclude unnecessary retries on specific network failure statuses
        if (error.message.includes('NOT_FOUND') || failureCount >= 3) {
          return false;
        }
        return true;
      }
    },
  },
});
```

```typescript
// src/core/state/useUIStore.ts
import { create } from 'zustand';

interface UIState {
  currentRoleView: 'dentist' | 'lab_admin' | 'technician' | 'clinic_admin';
  themePreference: 'light' | 'dark' | 'system';
  isNetworkIndicatorVisible: boolean;
  selectedCaseId: string | null;
  setRoleView: (role: 'dentist' | 'lab_admin' | 'technician' | 'clinic_admin') => void;
  setThemePreference: (pref: 'light' | 'dark' | 'system') => void;
  setSelectedCase: (caseId: string | null) => void;
}

export const useUIStore = create<UIState>((set) => ({
  currentRoleView: 'dentist',
  themePreference: 'system',
  isNetworkIndicatorVisible: false,
  selectedCaseId: null,
  setRoleView: (role) => set({ currentRoleView: role }),
  setThemePreference: (pref) => set({ themePreference: pref }),
  setSelectedCase: (caseId) => set({ selectedCaseId: caseId }),
}));
```

---

## 12. Database Architecture & Mappings

The data model connects high-throughput dental records across secure, decoupled nodes.

```
+--------------------+            +-------------------+            +---------------------+
|      CLINICS       |            |   ORGANIZATIONS   |            |        LABS         |
+--------------------+            +-------------------+            +---------------------+
| - id (PK)          |            | - id (PK)         |            | - id (PK)           |
| - name             |<-----------| - tenantType      |----------->| - name              |
| - address          |            | - status          |            | - specializations   |
| - partneredLabIds  |            +-------------------+            | - clinicPairings    |
+--------------------+                                             +---------------------+
          |                                                                   |
          | 1                                                                 | 1
          |                                                                   |
          | N                                                                 | N
+----------------------------------------------------------------------------------------+
|                                        CASES                                           |
+----------------------------------------------------------------------------------------+
| - id (PK)                                                                              |
| - patientName / patientId                                                              |
| - clinicId (FK)                                                                        |
| - labId (FK)                                                                           |
| - restorationType (e.g. "Crown", "Bridge")                                             |
| - shade (VITA Classic shade system code)                                               |
| - status (SCANNING -> DESIGNING -> PRODUCTION -> QC -> SHIPPED)                        |
| - dueDate / createdAt                                                                  |
| - notes (Prescription directions)                                                      |
+----------------------------------------------------------------------------------------+
          |                                                                   |
          | 1                                                                 | 1
          |                                                                   |
          | N                                                                 | N
+--------------------+                                             +---------------------+
|     SCANFILES      |                                             |    CHAT_CHANNELS    |
+--------------------+                                             +---------------------+
| - id (PK)          |                                             | - id (PK)           |
| - caseId (FK)      |                                             | - caseId (FK)       |
| - fileUrl (Storage)|                                             | - participants (Arr)|
| - fileType (stl/pdf)                                             | - messages (SubColl)|
+--------------------+                                             +---------------------+
```

---

## 13. Firestore Collections Schemas

This section outlines schema definitions for Firestore collections, validated client-side using `zod` to prevent malformed writes.

### `cases` Collection Schema
```typescript
// src/features/cases/types/index.ts
import { z } from 'zod';

export const CaseStatusSchema = z.enum([
  'SCANNING', 
  'DESIGNING', 
  'IN_PRODUCTION', 
  'QUALITY_CHECK', 
  'READY_FOR_PICKUP',
  'SHIPPED',
  'DELIVERED'
]);

export const ScanFileSchema = z.object({
  id: z.string(),
  fileName: z.string(),
  fileSize: z.number(), // in bytes
  fileType: z.enum(['stl', 'pdf', 'jpeg', 'png']),
  downloadUrl: z.string().url(),
  uploadedBy: z.string(),
  uploadedAt: z.string(),
});

export const CaseSchema = z.object({
  id: z.string(), // e.g. "DB-8829"
  patientId: z.string(),
  clinicId: z.string(),
  dentistId: z.string(),
  labId: z.string(),
  assignedTechnicianId: z.string().nullable(),
  restorationType: z.string(), // e.g., "Monolithic Zirconia Crown"
  toothNumber: z.array(z.string()), // ISO Tooth system e.g., ["14", "15"]
  shade: z.string(), // e.g. "A2", "B1"
  material: z.string(),
  notes: z.string().default(''),
  status: CaseStatusSchema,
  scanFiles: z.array(ScanFileSchema).default([]),
  dueDate: z.string(), // ISO String
  createdAt: z.string(), // ISO String
  updatedAt: z.string(), // ISO String
});

export type Case = z.infer<typeof CaseSchema>;
```

### `chats` Collection Schema
```typescript
export const MessageSchema = z.object({
  id: z.string(),
  senderId: z.string(),
  senderName: z.string(),
  senderRole: z.enum(['dentist', 'lab_admin', 'technician', 'clinic_admin']),
  text: z.string(),
  timestamp: z.any(), // Firebase Timestamp resolved mapping
  attachmentUrl: z.string().url().optional(),
});

export const ChatChannelSchema = z.object({
  id: z.string(), // Maps exactly to corresponding caseId (1:1 chat channel per Case)
  caseId: z.string(),
  participants: z.array(z.string()), // Array of uids (Dentist, Technicians, Admins)
  lastMessageText: z.string(),
  lastMessageTimestamp: z.any(),
});
```

---

## 14. Production Security Rules (Firestore & Storage)

Strict Role-Based Access Control (RBAC) ensuring data boundary isolation. Patients' Personally Identifiable Information (PII) is kept isolated within each designated clinic's tenant.

### Firestore Rules (`firestore.rules`)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }

    function getUserData() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data;
    }

    function hasRole(role) {
      return isAuthenticated() && getUserData().role == role;
    }

    function isMemberOfOrganization(orgId) {
      return isAuthenticated() && getUserData().organizationId == orgId;
    }

    // Users Collection rules
    match /users/{userId} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && request.auth.uid == userId;
    }

    // Clinic Organizations Collections
    match /clinics/{clinicId} {
      allow read: if isAuthenticated() && (isMemberOfOrganization(clinicId) || hasRole('lab_admin'));
      allow write: if isAuthenticated() && isMemberOfOrganization(clinicId) && hasRole('clinic_admin');
    }

    // Lab Organizations Collections
    match /labs/{labId} {
      allow read: if isAuthenticated() && (isMemberOfOrganization(labId) || hasRole('dentist'));
      allow write: if isAuthenticated() && isMemberOfOrganization(labId) && hasRole('lab_admin');
    }

    // Cases Collection rules (Strict boundaries between partners)
    match /cases/{caseId} {
      allow read: if isAuthenticated() && (
        isMemberOfOrganization(resource.data.clinicId) || 
        isMemberOfOrganization(resource.data.labId)
      );
      
      allow create: if isAuthenticated() && (
        isMemberOfOrganization(request.resource.data.clinicId) && 
        (hasRole('dentist') || hasRole('clinic_admin'))
      );
      
      allow update: if isAuthenticated() && (
        // Clinic edit rights
        (isMemberOfOrganization(resource.data.clinicId) && (hasRole('dentist') || hasRole('clinic_admin'))) ||
        // Lab edit rights
        (isMemberOfOrganization(resource.data.labId) && (hasRole('lab_admin') || hasRole('technician')))
      );

      allow delete: if false; // Regulatory audits strictly prevent deleting physical dental files
    }

    // Chat Channels Sub-collections
    match /chats/{channelId} {
      allow read, write: if isAuthenticated() && request.auth.uid in resource.data.participants;
      
      match /messages/{messageId} {
        allow read, write: if isAuthenticated() && request.auth.uid in get(/databases/$(database)/documents/chats/$(channelId)).data.participants;
      }
    }
  }
}
```

### Cloud Storage Rules (`storage.rules`)
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    function isAuthenticated() {
      return request.auth != null;
    }

    function isCaseParticipant(caseId) {
      return isAuthenticated() && 
        firestore.get(/databases/(default)/documents/cases/$(caseId)).data.clinicId == firestore.get(/databases/(default)/documents/users/$(request.auth.uid)).data.organizationId ||
        firestore.get(/databases/(default)/documents/cases/$(caseId)).data.labId == firestore.get(/databases/(default)/documents/users/$(request.auth.uid)).data.organizationId;
    }

    // CAD STL scans and clinical photos bucket
    match /cases/{caseId}/{fileName} {
      allow read: if isCaseParticipant(caseId);
      // Limit file uploads to STL digital scans, pdf prescriptions, or image snapshots
      allow write: if isCaseParticipant(caseId) && (
        fileName.matches('.*\\.stl') || 
        fileName.matches('.*\\.pdf') || 
        fileName.matches('.*\\.jpg') || 
        fileName.matches('.*\\.png')
      ) && request.resource.size < 150 * 1024 * 1024; // Strict cap of 150MB per STL scan file
    }
  }
}
```

---

## 15. Naming Conventions

To keep our codebase standardized and predictable:

- **Folders / Directories:**
  - Standard features and components use `kebab-case` (e.g., `cad-viewer`, `cases`).
- **Files:**
  - TypeScript modules and helpers use `camelCase` (e.g., `fcmService.ts`, `queryClient.ts`).
  - React components and custom React Context hooks use `PascalCase` (e.g., `NetInfoProvider.tsx`, `STLViewer.tsx`).
- **TypeScript Types & Interfaces:**
  - Standard `PascalCase` using descriptive nouns (e.g., `UserProfile`, `AppThemeType`).
- **Zustand Stores:**
  - Prefixed with `use` and written in camelCase (e.g., `useAuthStore`, `useUIStore`).
- **Database Collections:**
  - Pluralized lowercase nouns (e.g., `users`, `cases`, `chats`).

---

## 16. Coding Standards (ESLint & Prettier Configurations)

These guidelines align with industrial standards to verify code quality and consistency.

### `.eslintrc.json`
```json
{
  "root": true,
  "extends": [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:react/recommended",
    "plugin:react-hooks/recommended"
  ],
  "parser": "@typescript-eslint/parser",
  "plugins": ["@typescript-eslint", "react", "react-hooks"],
  "parserOptions": {
    "ecmaFeatures": {
      "jsx": true
    },
    "ecmaVersion": "latest",
    "sourceType": "module"
  },
  "rules": {
    "react/react-in-jsx-scope": "off",
    "react/prop-types": "off",
    "@typescript-eslint/no-unused-vars": ["error", { "argsIgnorePattern": "^_" }],
    "no-console": ["warn", { "allow": ["warn", "error"] }],
    "react-hooks/rules-of-hooks": "error",
    "react-hooks/exhaustive-deps": "warn"
  },
  "settings": {
    "react": {
      "version": "detect"
    }
  }
}
```

### `.prettierrc`
```json
{
  "arrowParens": "always",
  "bracketSameLine": false,
  "bracketSpacing": true,
  "singleQuote": true,
  "trailingComma": "all",
  "printWidth": 100,
  "tabWidth": 2,
  "semi": true
}
```

---

## 17. Scalability Plan & High-Throughput Strategy

As an enterprise-grade ecosystem, DentBridge is designed for high-availability performance:

1. **Incremental Loading of Large Payload files (STL):**
   - High-fidelity intraoral STL models average 50MB to 120MB. Rather than loading full assets instantly, the `cad-viewer` uses multi-part background downloading.
   - Leverages `expo-file-system` to download assets to the persistent local sandbox before passing standard cached file-paths to the rendering loop.
2. **Metadata Parsers:**
   - Pre-processes STL vertex counts, physical bounding-box dimensions, and volume calculations server-side (Cloud Functions) immediately post-upload.
   - The device downloads only light metadata parameters to render instant 2D preview projections, keeping 3D render processes memory-isolated.
3. **Optimized Firestore Indexes:**
   - Multi-tenant case lookups require complex filtering. Below is a list of required composite index structures:
     ```
     Collection: cases
     - Field: clinicId (Ascending) | status (Ascending) | dueDate (Descending)
     - Field: labId (Ascending) | status (Ascending) | dueDate (Descending)
     - Field: assignedTechnicianId (Ascending) | status (Ascending)
     ```
4. **FCM Multicast & Silent sync notifications:**
   - When status transitions occur on the server, silent push payloads are dispatched. This prompts the client application to sync queries in the background, updating active dashboards before user navigation.

---

## 18. Structured Development Roadmap

### Phase 1: IAM & Tenant Provisioning (Weeks 1-3)
- Initialize project with React Native (Expo Latest) and validate Env load schema.
- Implement Firebase Auth, multi-tenant sign-up forms, and role-based metadata bindings.
- Establish theme config and top-level navigation layout structure.

### Phase 2: Core Cases Management (Weeks 4-7)
- Establish the Firestore structure and secure RBAC rules.
- Set up React Hook Form wizard containing Zod prescriptions validation.
- Implement React Query integration for offline list caching and mutations.

### Phase 3: CAD Viewer & Storage (Weeks 8-10)
- Integrate custom native-friendly WebGL or Canvas-based STL mesh renders.
- Implement background chunked upload for intraoral digital scans to Cloud Storage.
- Set up automatic metadata-extraction via Cloud Functions.

### Phase 4: Chat & Collaborative Workflows (Weeks 11-12)
- Configure Real-time Firestore sub-collections mapping chats per Case.
- Implement dynamic keyboard-avoidance screens with in-app chat channels.
- Configure image sharing & push alert warnings via FCM.

### Phase 5: Verification & System Hardening (Weeks 13-14)
- Complete end-to-end integration testing for offline synchronization, conflict resolution, and RBAC edge-cases.
- Verify security rules, configure Firebase App Check to secure storage buckets, and run memory profile tracing for large mesh renders.

### Phase 6: Production Launch & Deployment (Week 15)
- Bundle standard APKs, configure automated OTA updates via Expo EAS, and publish internal TestFlight and Google Play distribution streams.
