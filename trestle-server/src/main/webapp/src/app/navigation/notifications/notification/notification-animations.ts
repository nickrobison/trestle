import {animate, AnimationTriggerMetadata, state, style, transition, trigger} from "@angular/animations";

export const notificationAnimations: {
  readonly fadeNotification: AnimationTriggerMetadata,
  readonly buttonNotification: AnimationTriggerMetadata,
} = {
  fadeNotification: trigger('fadeAnimation', [
    state('in', style({opacity: 1})),
    transition('void => *', [style({
      opacity: 0.5
    }),
      animate('{{ fadeIn }}ms')]),
    transition('default => closing', animate('{{ fadeOut }}ms', style({ opacity: 0 })),
      ),
  ]),
  buttonNotification: trigger('buttonAnimation', [
    state('out', style({opacity: 0.5})),
    transition('in => out', [style({
      opacity: 0.5
    }),
    animate('500ms')]),
    transition('out => in', [style({
        opacity: 1
      }),
      animate('500ms')]),
  ]),
};

export type NotificationAnimationState = 'default' | 'closing';

export type NotificationButtonState = 'in' | 'out';
