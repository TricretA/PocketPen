import React from 'react';
import {Composition} from 'remotion';
import {Intro} from './Intro';
import {About} from './About';

export const Root: React.FC = () => (
  <>
    <Composition id="Intro" component={Intro} durationInFrames={84} fps={30} width={720} height={1280} />
    <Composition id="About" component={About} durationInFrames={108} fps={30} width={720} height={720} />
  </>
);
