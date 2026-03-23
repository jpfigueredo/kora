# Angular – Contexto de Desenvolvimento (Kora)

Você está trabalhando com **Angular 18+** usando **standalone components** no projeto **Kora**.

## Arquitetura Frontend

```
src/app/
├── core/               # Singleton: guards, interceptors, layout
│   ├── auth/
│   ├── http/
│   └── layout/
├── shared/             # Reutilizáveis: components, pipes, directives
├── features/           # Feature modules (lazy loaded)
│   ├── identity/       # Login, Register, Profile
│   ├── social/         # Connections
│   ├── content/        # Feed, Post, Comments
│   └── community/      # CommunityPage, Events, Blog
└── app.routes.ts
```

## Padrões

### Standalone Component com Signals
```typescript
@Component({
  selector: 'kora-feed',
  standalone: true,
  imports: [CommonModule, RouterModule, KoraPostCardComponent],
  templateUrl: './feed.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeedComponent {
  private feedService = inject(FeedService);
  posts = signal<Post[]>([]);
  isLoading = signal(false);
}
```

### Control Flow (Angular 17+)
```html
@for (post of posts(); track post.id) {
  <kora-post-card [post]="post" />
}
@if (isLoading()) {
  <kora-spinner />
}
```

### Service
```typescript
@Injectable({ providedIn: 'root' })
export class FeedService {
  private http = inject(HttpClient);
  getFeed(page: number, size: number): Observable<Post[]> {
    return this.http.get<Post[]>('/api/v1/feed', { params: { page, size } });
  }
}
```

### DI, Forms, Routes
```typescript
// DI: sempre inject()
private router = inject(Router);

// Forms: reactive
form = inject(FormBuilder).group({
  email: ['', [Validators.required, Validators.email]],
});

// Routes: lazy loading
{ path: 'feed', loadComponent: () => import('./features/content/feed/feed.component').then(m => m.FeedComponent) }
```

### Auth Guard e Interceptor
```typescript
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? true : router.createUrlTree(['/login']);
};

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).getToken();
  return token ? next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })) : next(req);
};
```

## Convenções Kora
- Standalone components, sem NgModule
- Signals para estado local, RxJS para async
- `inject()` em vez de constructor injection
- `@for`/`@if` novo control flow
- TypeScript strict, sem `any`
- OnPush change detection
- Lazy loading por feature
- Prefix `kora-` para shared components
- Mobile-first responsive
