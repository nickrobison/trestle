import {FactTableComponent} from './fact-table.component';
import {moduleMetadata} from '@storybook/angular';
import {MaterialModule} from '../../../../material/material.module';
import {TrestleFact} from '../../../../shared/individual/TrestleIndividual/trestle-fact';
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";

export default {
  title: 'Fact Table',
  excludeStories: /.*Data$/,
  decorators: [
    moduleMetadata({
      declarations: [FactTableComponent],
      imports: [MaterialModule, BrowserAnimationsModule]
    })
  ]
};

const fact1 = [
  new TrestleFact({
    identifier: 'http://test.id1',
    name: 'Test Fact',
    type: 'String',
    value: 'Test Fact Value',
    validTemporal: {
      ID: 'Not really, real',
      From: new Date('1989-03-26'),
      To: new Date('1990-05-11')
    },
    databaseTemporal: {
      ID: 'Not really, real',
      From: new Date('2020-01-01')
    }
  }),
  new TrestleFact({
    identifier: 'http://test.id2',
    name: 'Test Fact',
    type: 'String',
    value: 'Test Fact Value (old)',
    validTemporal: {
      ID: 'Not really, real',
      From: new Date('1989-03-26'),
      To: new Date('1990-05-11')
    },
    databaseTemporal: {
      ID: 'Not really, real',
      From: new Date('2019-01-01'),
      To: new Date('2020-01-01')
    }
  })
];

// eslint-disable-next-line @typescript-eslint/naming-convention
export const Default = () => ({
  component: FactTableComponent,
  props: {
    facts: fact1
  }
});
