import dayjs from 'dayjs/esm';

import { IUserManagement, NewUserManagement } from './user-management.model';

export const sampleWithRequiredData: IUserManagement = {
  login: 'Diego.ManzanaresRomo',
  email: 'Margarita5@hotmail.com',
};

export const sampleWithPartialData: IUserManagement = {
  id: 18824,
  login: 'Guadalupe.LaboyMoreno4',
  firstName: 'Miguel',
  lastName: 'Navarrete Carrera',
  email: 'Graciela44@hotmail.com',
  activated: false,
  langKey: 'es',
  lastModifiedBy: 'schematise hm',
  lastModifiedDate: dayjs('2026-10-28T18:21'),
};

export const sampleWithFullData: IUserManagement = {
  id: 26558,
  login: 'Catalina72',
  firstName: 'Micaela',
  lastName: 'Apodaca Armendáriz',
  email: 'Alfredo_CardenasAdame65@hotmail.com',
  activated: false,
  langKey: 'es',
  imageUrl: 'as vastly than',
  createdBy: 'out absentmindedly',
  createdDate: dayjs('2026-10-28T22:36'),
  lastModifiedBy: 'but sour sympathetically',
  lastModifiedDate: dayjs('2026-10-28T18:29'),
};

export const sampleWithNewData: NewUserManagement = {
  email: 'JuanCarlos82@yahoo.com',
  login: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
