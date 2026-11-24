import React from 'react';
import {Composition} from 'remotion';
import {Intro} from './Intro';

export const Root: React.FC = () => (
  <Composition id="Intro" component={Intro} durationInFrames={84} fps={30} width={720} height={1280} />
);
